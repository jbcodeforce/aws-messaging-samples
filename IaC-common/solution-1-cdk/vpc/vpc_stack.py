from aws_cdk import (
    # Duration,
    Stack,
    aws_ec2 as ec2,
)
from constructs import Construct

cidr="10.10.0.0/16"

class VpcStack(Stack):

    def __init__(self, scope: Construct, construct_id: str, **kwargs) -> None:
        super().__init__(scope, construct_id, **kwargs)

        search = self.lookup_vpc(vpc_name)
        if search == None:
            self.vpc=self.create_vpc(vpc_name)
            self.add_security_groups(self.vpc)
        else:
            print("found vpc")
            self.vpc=ec2.Vpc.from_lookup(self, "lookup", vpc_name=vpc_name, is_default=False)
        print(self.vpc.vcp_id)
        CfnOutput(self,"VPC", value=self.vpc.vpc_id, export_name="vpc")

    
    def lookup_vpc(self, vpc_name: str) -> ec2.Vpc:
        client = boto3.client('ec2')
        response = client.describe_vpcs(
                        Filters=[{
                            'Name': 'tag:Name',
                            'Values': [
                                vpc_name
                            ]
                        }]
                    )
        print(response)
        if len(response['Vpcs']) > 0:
            vpc=response['Vpcs'][0]
        else:
            vpc= None
        return vpc
    
    def create_vpc(self, vpc_name: str) -> ec2.Vpc:
        vpc = ec2.Vpc(self, vpc_name,
            max_azs=3,
            vpc_name=vpc_name,
            nat_gateways=3,
            ip_addresses=ec2.IpAddresses.cidr(CIDR),
            subnet_configuration=[
                ec2.SubnetConfiguration(
                    name="public",
                    subnet_type=ec2.SubnetType.PUBLIC,
                    cidr_mask=cidr_mask
                ),
                ec2.SubnetConfiguration(
                    name="private",
                    subnet_type=ec2.SubnetType.PRIVATE_WITH_EGRESS,
                    cidr_mask=cidr_mask
                )
            ]
        )
        return vpc
    
    def add_security_groups(self, vpc: ec2.Vpc) -> None:
        print("add security groups")

    def getCIDR(self) -> str:
        return CIDR
        self.vpc = ec2.Vpc(self, "VPC",
                           max_azs=2,
                           cidr=cidr,
                           nat_gateways=2,
                           enable_dns_hostnames=True,
                           enable_dns_support=True,
                           subnet_configuration=[
                               ec2.SubnetConfiguration(
                                   name="public",
                                   subnet_type=ec2.SubnetType.PUBLIC,
                                   cidr_mask=24),
                               ec2.SubnetConfiguration(
                                   subnet_type=ec2.SubnetType.PRIVATE_WITH_EGRESS,
                                   name="private",
                                   cidr_mask=24) # could be /16 to have more instances, but this is a demo scope.
                           ]
                        )
