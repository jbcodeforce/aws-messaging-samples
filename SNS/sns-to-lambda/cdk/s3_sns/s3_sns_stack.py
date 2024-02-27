from aws_cdk import (
    Duration,
    Stack,
    RemovalPolicy,
    CfnOutput,
    aws_s3 as s3,
    aws_sns,
    aws_sns_subscriptions,
    aws_lambda,
    aws_s3_notifications as s3n
)
from constructs import Construct

class S3NotifToSnsStack(Stack):
    """
    Create S3 Bucket with an event notification to SNS Topic
    """
    def __init__(self, scope: Construct, construct_id: str, **kwargs) -> None:
        super().__init__(scope, construct_id, **kwargs)

        # create s3 bucker with event notification to sns
        topic = aws_sns.Topic(self, 
                          'tenant-grp-2',
                          topic_name='tenant-grp-2',
                          display_name='tenant-grp-2')

        bucket = s3.Bucket(self, "tenant-group-2",
                           bucket_name='tenant-group-2',
                           versioned=False,
                           removal_policy=RemovalPolicy.DESTROY,
                           auto_delete_objects=True)
        bucket.add_event_notification(s3.EventType.OBJECT_CREATED, 
                                    s3n.SnsDestination(topic))

        fct = aws_lambda.Function(self, "S3NotifProcessing",
                                  runtime=aws_lambda.Runtime.PYTHON_3_12,
                                  handler="app.lambda_handler",
                                  code=aws_lambda.Code.from_asset("../s3_event_routing"))

        # add function subscriber to topic
        topic.add_subscription(aws_sns_subscriptions.LambdaSubscription(fn=fct,
                                                     filter_policy=None))

        # Output information about the created resources
        CfnOutput(self, 'snsTopicArn',
                      value=topic.topic_arn,
                      description='The ARN of the SNS Topic')
        CfnOutput(self, 'bucketName',
                      value=bucket.bucket_name,
                      description='The name of the bucket created')
        CfnOutput(self, 'functionArn',
                      value=fct.function_arn,
                      description='The ARN of the Lambda function.')


