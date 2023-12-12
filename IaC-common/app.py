#!/usr/bin/env python3
import os

import aws_cdk as cdk
import aws_cdk.aws_ec2 as ec2

from vpc.vpc_stack import VpcStack
from cloud9.cloud9_stack import Cloud9Stack

def add_security_groups(vpc: ec2.Vpc) -> None:
        sg = ec2.SecurityGroup("AMQSecurityGroup",
                    vpc=vpc,
                    security_group_name="AMQ-SG",
                    description="Allow Active MQ",
                    allow_all_outbound=True,
                    disable_inline_rules=False
        )
        sg.add_ingress_rule(ec2.Peer.any_ipv4(),ec2.Port.tcp(8162),"Active MQ console access")
        sg.add_ingress_rule(ec2.Peer.any_ipv4(),ec2.Port.tcp(22),"ssh from cloud9")
        print("add security groups")
        return sg

app = cdk.App()
demo_vpc=VpcStack(app, "DemoVpc",
    env=cdk.Environment(account=os.getenv('CDK_DEFAULT_ACCOUNT'), region=os.getenv('CDK_DEFAULT_REGION')),
)


c9=Cloud9Stack(app, 
            "Cloud9Stack", 
            vpc=demo_vpc.vpc,
            env=cdk.Environment(account=os.getenv('CDK_DEFAULT_ACCOUNT'), region=os.getenv('CDK_DEFAULT_REGION'))
            )

app.synth()
