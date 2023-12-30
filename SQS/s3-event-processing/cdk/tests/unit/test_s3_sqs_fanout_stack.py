import aws_cdk as core
import aws_cdk.assertions as assertions

from s3_sqs_fanout.s3_sqs_fanout_stack import S3SqsFanoutStack

# example tests. To run these tests, uncomment this file along with the example
# resource in s3_sqs_fanout/s3_sqs_fanout_stack.py
def test_sqs_queue_created():
    app = core.App()
    stack = S3SqsFanoutStack(app, "s3-sqs-fanout")
    template = assertions.Template.from_stack(stack)

#     template.has_resource_properties("AWS::SQS::Queue", {
#         "VisibilityTimeout": 300
#     })
