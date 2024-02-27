#!/usr/bin/env python3
import os

import aws_cdk as cdk

from s3_sns.s3_sns_stack import S3NotifToSnsStack


app = cdk.App()
S3NotifToSnsStack(app, "S3EvtNotifToSnsStack",
   env=cdk.Environment(account=os.getenv('CDK_DEFAULT_ACCOUNT'), region=os.getenv('CDK_DEFAULT_REGION')),
    )

app.synth()
