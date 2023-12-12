from aws_cdk import (
    # Duration,
    Stack,
    aws_amazonmq as amazonmq,
    aws_ec2 as ec2 ,
    Fn,
    CfnOutput
)
from constructs import Construct
import boto3

ENGINE_TYPE="ACTIVEMQ"


def getSubnetIds(vpc_name):
    ec2 = boto3.client('ec2')
    response = ec2.describe_vpcs(Filters=[
        {
            'Name': 'tag:Name',
            'Values': [vpc_name]
        }
    ])
    vpc_id=response['Vpcs'][0]['VpcId']
    print("The vpc id is " + vpc_id)
   
    response = ec2.describe_subnets(Filters=[{'Name': 'vpc-id', 'Values': [vpc_id]},
                         {'Name': 'tag:aws-cdk:subnet-type', 'Values': ['Private']} ])
    subnetIds = []
    for subnet in response['Subnets']:
        print(subnet)
        subnetIds.append(subnet['SubnetId'])
    return subnetIds

class ActiveStandbyStack(Stack):

    def add_security_groups(self,vpc: ec2.Vpc) -> None:
        sg = ec2.SecurityGroup(self,"AMQSecurityGroup",
                    vpc=vpc,
                    security_group_name="AMQ-SG",
                    description="Allow Active MQ",
                    allow_all_outbound=True,
                    disable_inline_rules=False
        )
        sg.add_ingress_rule(ec2.Peer.any_ipv4(),ec2.Port.tcp(8162),"Active MQ console access")
        sg.add_ingress_rule(ec2.Peer.any_ipv4(),ec2.Port.tcp(22),"ssh from cloud9")
        print("add security groups")
        CfnOutput(self, "AMQSecurityGroup-out", value=sg.security_group_id)
        return sg
    
    def defineConfiguration(self, version):
        with open("./activeMQ/broker-config.xml") as f:
            data = f.read()
        cfn_configuration = amazonmq.CfnConfiguration(self, "MyCfnConfiguration",
            data=data,
            engine_type=ENGINE_TYPE,
            engine_version=version,
            name="demo-jb-configuration",
            # the properties below are optional
            authentication_strategy="SIMPLE",
            description="description",
            tags=[amazonmq.CfnConfiguration.TagsEntryProperty(
                                    key="SolutionName",
                                    value="Demo"
                                ),
                amazonmq.CfnConfiguration.TagsEntryProperty(
                                    key="Environment",
                                    value="dev"
                                )
                ],
        )
        return cfn_configuration
    
    def __init__(self, scope: Construct, construct_id: str, vpc_name: str, amq_version: str, **kwargs) -> None:
        super().__init__(scope, construct_id, **kwargs)
        subnetIds = getSubnetIds(vpc_name)
        vpc=ec2.Vpc.from_lookup(self, "lookup", vpc_name=vpc_name, is_default=False)
        self.defineConfiguration(amq_version)
        securityGroups = [self.add_security_groups(vpc).security_group_id]
        active_broker = amazonmq.CfnBroker(self, "ActiveBroker",
                            auto_minor_version_upgrade=False,
                            broker_name="j9r-demo",
                            #deployment_mode="ACTIVE_STANDBY_MULTI_AZ",
                            deployment_mode="SINGLE_INSTANCE",
                            engine_type=ENGINE_TYPE,
                            engine_version=amq_version,
                            publicly_accessible=True,
                            host_instance_type="mq.t3.micro",
                            authentication_strategy="SIMPLE",
                            users=[amazonmq.CfnBroker.UserProperty(
                                password="adminpassw0rd",
                                username="admin",

                                # the properties below are optional
                                console_access=True,
                                groups=["admin"]
                            )],
                            configuration=amazonmq.CfnBroker.ConfigurationIdProperty(
                                id="demo-jb-configuration",
                                revision=1
                            ),
                            tags=[amazonmq.CfnBroker.TagsEntryProperty(
                                    key="SolutionName",
                                    value="Demo"
                                ),amazonmq.CfnBroker.TagsEntryProperty(
                                    key="Environment",
                                    value="dev"
                                )],
                            logs=amazonmq.CfnBroker.LogListProperty(
                                audit=False,
                                general=True
                            ),
                            subnet_ids=subnetIds,
                            security_groups=securityGroups
                        )
       
        CfnOutput(self, "ActiveBroker-out", value=Fn.select(0,active_broker.attr_open_wire_endpoints))
        '''
       
        standby_broker = amazonmq.CfnBroker(self, "StandbyBroker",
                            auto_minor_version_upgrade=False,
                            broker_name="demo-standby",
                            deployment_mode="ACTIVE_STANDBY_MULTI_AZ",
                            engine_type="ACTIVEMQ",
                            engine_version=amq_version,
                            host_instance_type="mq.t3.micro",
                            publicly_accessible=True,
                            authentication_strategy="SIMPLE",
                            users=[amazonmq.CfnBroker.UserProperty(
                                password="adminpassw0rd",
                                username="admin",

                                # the properties below are optional
                                console_access=True,
                                groups=["admin"]
                            )],
                            logs=amazonmq.CfnBroker.LogListProperty(
                                audit=False,
                                general=True
                            ),
                            subnet_ids=[subnetIds[1]],

                        )
         '''
        
