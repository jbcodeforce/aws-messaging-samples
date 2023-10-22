import aws_cdk as core
import aws_cdk.assertions as assertions

from active_standby.active_standby_stack import ActiveStandbyStack

# example tests. To run these tests, uncomment this file along with the example
# resource in active_standby/active_standby_stack.py
def test_sqs_queue_created():
    app = core.App()
    stack = ActiveStandbyStack(app, "active-standby")
    template = assertions.Template.from_stack(stack)

#     template.has_resource_properties("AWS::SQS::Queue", {
#         "VisibilityTimeout": 300
#     })
