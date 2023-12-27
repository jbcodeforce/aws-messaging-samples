import boto3, os

queue_name = os.environ.get('SQS_QUEUE_NAME')
sqs = boto3.resource('sqs')
queue = sqs.get_queue_by_name(QueueName=queue_name)

for message in queue.receive_messages():
    print(message.body)
    message.delete()