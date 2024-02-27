# [Amazon SNS - Simple Notification Service](https://docs.aws.amazon.com/sns/latest/dg/welcome.html)


???- info "Amazon SNS elevator pitch"
    Amazon SNS is a highly available, durable, secure, fully managed pub/sub messaging service that enables you to decouple microservices, distributed systems, and serverless applications. Amazon SNS provides topics for high-throughput, push-based, many-to-many messaging.

Producer sends message to one SNS Topic. SNS pushes data to subscribers. 

SNS supports up to 12,500,000 subscriptions per topic, 100,000 topic limits per account. For subscription filter policies, the default _per topic_ limit, per AWS account, is 200. The filter policies' limit per AWS account is 10,000.

[Priced by](https://aws.amazon.com/sns/pricing/) number of messages published, the number of notifications, and number of API calls. 

## Producing messages

The producers can publish to topic via SDK or can use different protocols like: HTTP / HTTPS (with delivery retries – how many times), SMTP,  SMS, ... 

Many AWS services can send data directly to SNS for notifications: CloudWatch (for alarms), AWS budget, Lambda, Auto Scaling Groups notifications, Amazon S3 (on bucket events), DynamoDB, CloudFormation, AWS Data Migration Service, RDS Event...

SNS messages may be archived via adding Amazon Data Firehose as a subscriber, then the destination becomes S3, Redshift tables, OpenSearch... Message replay is then custom.

SNS FIFO topics support an in-place, no-code, message archive that lets topic owners store (or archive) messages published to a topic for up to 365 days.

## Consuming messages

Each subscriber to the topic will get all the messages. As data is not persisted, we may lose messages not processed within a specific time window.

SNS can filter message by using a JSON policy attached to the SNS topic's subscription. By default all messages go to subscribers. SNS compares the message attributes or the message body to the properties in the filter policy for each of the topic's subscriptions, in case of match the message is sent to the subscriber. See some [filter policy examples](https://docs.aws.amazon.com/sns/latest/dg/example-filter-policies.html).

The subscribers can be a SQS queue, a HTTPs endpoint, a Lambda function, Kinesis Firehose, EventBridge, Emails... But not Kinesis Data Streams.

SNS in EDA can be used by adding subscription to event-handling pipelines—powered by [AWS Event Fork Pipelines](https://docs.aws.amazon.com/sns/latest/dg/sns-fork-pipeline-as-subscriber.html). [See samples here.](https://docs.aws.amazon.com/sns/latest/dg/example-sns-fork-use-case.html)

???- remarks "Eventual consistency"
    Change to a subscription filter policy require up to 15 minutes to fully take effect. This duration can't be reduced.

## Security

For security needs, it supports HTTPS, and encryption at rest with KMS keys. For access control, IAM policies can be defined for the SNS API (looks like S3 policies). Same as SQS, used for cross account access and with other services.

## Combining with SQS - Fan Out pattern

SNS can be combined with SQS: Producers push once in SNS, and messages are receive in all SQS queues, subscribers to the topic It is fully decoupled without any data loss. SQS allows for data persistence, delayed processing and retries.

* An application pushes once in a SNS Service, and the SQS queues are subscribers to the topic and then get the messages. Fan Out.
* Fully decoupled with no data loss as SQS will always listen to SNS topic.
* SQS adds data persistence, delayed processing and retries of work.
* Increase the number of subscriber over time.
* Using SNS FIFO and SQS FIFO it will keep ordering.
* Can use Message Filtering using a JSON policy .
* Can be used for cross-region delivery, with a SQS queue in another region.