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
