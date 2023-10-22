#!/usr/bin/env python3
import os

import aws_cdk as cdk

from vpc.vpc_stack import VpcStack
from cloud9.cloud9_stack import Cloud9Stack



app = cdk.App()
demo_vpc=VpcStack(app, "DemoVpc",
    env=cdk.Environment(account=os.getenv('CDK_DEFAULT_ACCOUNT'), region=os.getenv('CDK_DEFAULT_REGION')),
)


Cloud9Stack(app, 
            "Cloud9Stack", 
            vpc=demo_vpc.vpc,
            env=cdk.Environment(account=os.getenv('CDK_DEFAULT_ACCOUNT'), region=os.getenv('CDK_DEFAULT_REGION'))
            )

app.synth()
