# Amazon SNS - Simple Notification Service

It is a Serverless service. Used for pub/sub communication. Producer sends message to one SNS Topic. SNS pushes data to subscribers. SNS supports up to 12,500,000 subscriptions per topic, 100,000 topic limit.

Each subscriber to the topic will get all the messages. As data is not persisted, we may lose messages not processed in a time window.

The producers can publish to topic via SDK and can use different protocols like: HTTP / HTTPS (with delivery retries – how many times), SMTP,  SMS, ... 

SNS can filter message (a JSON policy attached to the SNS topic's subscription).

The subscribers can be a SQS queue, a HTTPs endpoint, a Lambda function, Kinesis Firehose, Emails... But not Kinesis Data Streams.

Many AWS services can send data directly to SNS for notifications: CloudWatch (for alarms), AWS budget, Lambda, Auto Scaling Groups notifications, Amazon S3 (on bucket events), DynamoDB, CloudFormation, AWS Data Migration Service, RDS Event...

SNS can be combined with SQS: Producers push once in SNS, receive in all SQS queues that they subscribed to. It is fully decoupled without any data loss. SQS allows for data persistence, delayed processing and retries. SNS cannot send messages to SQS FIFO queues.

For security it supports HTTPS, and encryption at rest with KSM keys. For access control, IAM policies can be defined for the SNS API (looks like S3 policies). Same as SQS, used for cross account access and with other services. 

### Combining with SQS - Fan Out pattern

* An application pushes once in a SNS Service, and the SQS queues are subscribers to the topic and then get the messages. Fan Out.
* Fully decoupled with no data loss as SQS will always listen to SNS topic.
* SQS adds data persistence, delayed processing and retries of work.
* Increase the number of subscriber over time.
* Using SNS FIFO and SQS FIFO it will keep ordering.
* Can use Message Filtering using a JSON policy .
* Can be used for cross-region delivery, with a SQS queue in another region.