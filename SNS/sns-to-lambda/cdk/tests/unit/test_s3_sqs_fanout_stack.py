import aws_cdk as core
import aws_cdk.assertions as assertions

from s3_sns.s3_sns_stack import S3NotifToSnsStack

# example tests. To run these tests, uncomment this file along with the example
# resource in s3_sns/s3_sns_stack.py
def test_sqs_queue_created():
    app = core.App()
    stack = S3NotifToSnsStack(app, "s3-sns")
    template = assertions.Template.from_stack(stack)

    template.has_resource_properties("AWS::SNS::Topic", {
         "TopicName": "tenant-grp-2"
    })
