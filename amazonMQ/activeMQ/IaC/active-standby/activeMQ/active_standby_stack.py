from aws_cdk import (
    # Duration,
    Stack,
    aws_amazonmq as amazonmq,
    CfnOutput
)
from constructs import Construct
import boto3

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

    def __init__(self, scope: Construct, construct_id: str, vpc_name: str, **kwargs) -> None:
        super().__init__(scope, construct_id, **kwargs)
        subnetIds = getSubnetIds(vpc_name)
        active_broker = amazonmq.CfnBroker(self, "ActiveBroker",
                            auto_minor_version_upgrade=False,
                            broker_name="demo-active",
                            deployment_mode="ACTIVE_STANDBY_MULTI_AZ",
                            engine_type="ACTIVEMQ",
                            engine_version="5.17.3",
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
                            logs=amazonmq.CfnBroker.LogListProperty(
                                audit=False,
                                general=True
                            ),
                            subnet_ids=[subnetIds[0]],
                        )
        standby_broker = amazonmq.CfnBroker(self, "StandbyBroker",
                            auto_minor_version_upgrade=False,
                            broker_name="demo-standby",
                            deployment_mode="ACTIVE_STANDBY_MULTI_AZ",
                            engine_type="ACTIVEMQ",
                            engine_version="5.17.3",
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
        
