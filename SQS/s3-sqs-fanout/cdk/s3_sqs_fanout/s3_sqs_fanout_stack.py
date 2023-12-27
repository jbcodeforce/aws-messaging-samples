from aws_cdk import (
    Duration,
    Stack,
    RemovalPolicy,
    CfnOutput,
    aws_s3 as s3,
    aws_sqs as sqs,
    aws_s3_notifications as s3n
)
from constructs import Construct

class S3SqsFanoutStack(Stack):

    def __init__(self, scope: Construct, construct_id: str, **kwargs) -> None:
        super().__init__(scope, construct_id, **kwargs)

        # create s3 bucker with event notification to sqs
        queue = sqs.Queue(self, 
                          'tenant-grp-1',
                          queue_name='tenant-grp-1',
                          retention_period=Duration.days(1),
                          encryption=sqs.QueueEncryption.UNENCRYPTED)

        bucket = s3.Bucket(self, "tenant-group-1",
                           bucket_name='tenant-group-1',
                           versioned=False,
                           removal_policy=RemovalPolicy.DESTROY,
                           auto_delete_objects=True)
        bucket.add_event_notification(s3.EventType.OBJECT_CREATED, 
                                    s3n.SqsDestination(queue))

        
        # Output information about the created resources
        CfnOutput(self, 'sqsQueueUrl',
                      value=queue.queue_url,
                      description='The URL of the SQS queue')
        CfnOutput(self, 'bucketName',
                      value=bucket.bucket_name,
                      description='The name of the bucket created')


