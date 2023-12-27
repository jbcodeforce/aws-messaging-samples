#!/usr/bin/env python3
import os

import aws_cdk as cdk

from s3_sqs_fanout.s3_sqs_fanout_stack import S3SqsFanoutStack


app = cdk.App()
S3SqsFanoutStack(app, "S3SqsFanoutStack",
   env=cdk.Environment(account=os.getenv('CDK_DEFAULT_ACCOUNT'), region=os.getenv('CDK_DEFAULT_REGION')),
    )

app.synth()
