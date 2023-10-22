#!/usr/bin/env python3
import os

import aws_cdk as cdk

from activeMQ.active_standby_stack import ActiveStandbyStack


app = cdk.App()
ActiveStandbyStack(app, "ActiveStandbyStack", "DemoVpc",
   env=cdk.Environment(account=os.getenv('CDK_DEFAULT_ACCOUNT'), region=os.getenv('CDK_DEFAULT_REGION')),
)

app.synth()
