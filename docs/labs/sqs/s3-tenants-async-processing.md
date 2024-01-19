# Process S3 events for multi-tenant bucket with EDA

???+ Info
    **Created:** Dec 2023 - Updated: 01/18/24. State: Draft

This study reviews how to best support the Amazon S3 Event Notification processing using an event-driven processing approach.

I cover different messaging technology like Amazon SQS, AWS Event Bridge or  streaming platform like Kafka.

The goal is to help developers or solution architects review the possible solutions and select the most appropriate one to address their own problem. The problem is generic enough to get a reusable solution.

## Introduction

This article is to mockup a SaaS multi-tenant application using S3 as Data Lake and an asynchronous event-driven processing to address S3 put or update object event notifications supporting 100k+ tenants.

* Each bucket has top level prefixes assigned to a unique tenant. Within a prefix, user can have a hierarchy of 'folders' and objects. It is a multi-tenancy segregation pattern using at the prefix level. The following figure illustrates a simple example of bucket and prefix per tenant organization:

```
tenant-group-1 (bucket)
├── tenant_1
│   ├── raw
│   │   ├── object_10
│   │   └── object_11
│   └── silver
│   │   ├── object_20
│   │   └── object_21
├── tenant_2
└── tenant_3
```

* To control file upload, a tool will take into account the tenant unique identifier to define the target S3 object prefix.
* The solution needs to support million of files uploaded to S3 per day, at a rate of 200k file moves per minute. Once a file is uploaded (in raw prefix), there are file processors that transform the file into another format (the demonstration in this repository, uses Iceberg) to save in another prefix (silver). The basic processing looks like in figure below:

![](./diagrams/s3event-processing.drawio.png)

**Figure 1: Single tenant processing**

* The event-driven processor can directly get message from the SQS queue, using AWS SDK, or being a Apache Spark application using S3 connector. It has to manage idempotency.
* The SaaS platform needs to support hundred of those event-driven processing in parallel. 
* The SaaS AWS account is the owner of the buckets, the queues, or topics created in the event backbones used for the solution.

### Constraints and constructs

* An AWS account can have a maximum of 100 buckets per region per account. This is a soft limit with a max of 1000 after change request to AWS.
* There is no limit on the number of objects stored within a bucket. No limit in the number of prefixes. However, a spike in the API request rate might cause throttling. In S3, partitions exist at the prefix level, and not at the object level. S3 doesn't use hierarchy to organize its objects and files. A folder is the value between the two slash (/) characters within the prefix name.
* S3 supports configuring event notifications on a **per-bucket** basis, to send events to other AWS services like AWS Lambda, SNS or SQS.
* To avoid looping, event notification needs to take into account the prefixes. We could use another bucket for the target of the file processing, but to keep tenancy within the constraint of a unique bucket, prefixes are used.
* S3 storage class is `Standard` or `S3 Express one Zone`.
* Each S3 bucket can have up to 100 event notification configurations. A configuration defines the event types to send from S3 and what filters to apply to the event by using the prefix and suffix attributes of the S3 objects:

    ![](./images/evt-noti-evt-types.png)

    **Figure 2: Defining S3 event notification in AWS Console**

    With the potential destination to publish the event:

    ![](./images/evt-notif-dest.png)

     **Figure 3: Destination for the events** 

* Below is an example of [Python AWS CDK](https://docs.aws.amazon.com/cdk/api/v2/python/aws_cdk.aws_s3_notifications.html) code to define event notification configuration for ObjectCreated event on bucket `tenant-group-1`, prefix: `tenant-1`.

    ```python
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
                                s3n.SqsDestination(queue),
                                NotificationKeyFilter( prefix="tenant-1/raw")
                                )
    ```

* Notifications are asynchronous: S3 will queue events and retry delivery if destinations are unavailable. This avoids blocking the caller.
* For demonstration purpose, we will process ObjectCreated events. See [the other supported notifications for SQS](https://docs.aws.amazon.com/AmazonS3/latest/userguide/notification-how-to-event-types-and-destinations.html#supported-notification-event-types) and [for the ones for EventBridge](https://docs.aws.amazon.com/AmazonS3/latest/userguide/EventBridge.html). Also CDK is quite static and works well with infrastructure as code and CI/CD practices. For this demonstration SDK may be a better solution to demonstrate the flexibility of the SaaS platform: we can imagine a `create tenant` API that provisions a prefix and assigns it to a bucket dynamically.
* On rare occasion S3 retry mechanism might cause duplicate S3 event notification.
* Event ordering: S3 event notification to SQS FIFO is not supported. Event Notification includes a Sequencer attribute which can be used to determine the order of events for a given object key. Sequencer provides a way to determine the sequence of events. Notifications from events that create objects (PUTs) and delete objects contain a sequencer.

### Limits

* Number of bucket per region per account: 100, with hard limit of 1000.
* S3 request performance: 3,500 PUT/COPY/POST/DELETE or 5,500 GET/HEAD requests per second per partitioned Amazon S3 prefix. So in this example, it will be per tenant. 200k file upload per minute should create around 3350 events per second.
* Number of S3 Event Notification: 100 events.
* S3 event notifications do not natively support event batching.

### Event Content

The S3 [Event notification includes metadata](https://docs.aws.amazon.com/AmazonS3/latest/userguide/notification-content-structure.html) about the bucket access done and the object created (*not all fields are reported in following json*):  

```json
{
    "Records": [
        {
            "eventVersion": "2.1",
            "eventSource": "aws:s3",
            "awsRegion": "us-west-2",
            "eventTime": "2023-12-22T01:13:20.539Z",
            "eventName": "ObjectCreated:Put",
            "userIdentity": {
                "principalId": "..."
            },
            "s3": {
                "s3SchemaVersion": "1.0",
                "configurationId": ".....jU4",
                "bucket": {
                    "name": "tenantgroup1958dab46-bv3cwcddfgox",
                    "ownerIdentity": {
                        "principalId": "..."
                    },
                    "arn": "arn:aws:s3:::tenantgroup1958dab46-bv3cwcddfgox"
                },
                "object": {
                    "key": "raw/reports.csv",,
                    "eTag": "1388e3face4eb9892960e366bd8b79aa",
                    "sequencer": "006584E2B074B0899D"
                }
            }
        }
    ]
}
```

Example of Python code to access the object's path,

```python
 for record in events["Records"]:
    bucket = record["s3"]["bucket"]
    objectName=record["s3"]["object"]["key"]
    print(bucket["name"]+"/"+objectName)
```

## Potential Solutions

In the figure 1 above, we were using SQS queue to support the asynchronous event processing with persistence. As there will be multi-tenant per bucket, we need to fanout the processing per tenant.

### Defining a group of tenants

Below is a simple tenant group definition for one S3 bucket and one unique queue:

```json
{
   "name": "tenant-group-1",
   "bucket": "<ACCOUNTID>-tenant-group-1",
   "region": "us-west-2",
   "queueURL": "https://sqs.us-west-2.amazonaws.com/ACCOUNTID/tenant-group-1",
   "queueArn": "arn:aws:sqs:us-west-2:ACCOUNTID:tenant-group-1",
   "status": "ACTIVE"
}
```

See the code to [create tenant group](https://github.com/jbcodeforce/aws-messaging-study/blob/main/SQS/s3-sqs-fanout/createGroupTenant.py).

When onboarding a tenant, the platform defines a unique tenant_id, and links it to the target bucket and prefix within a persisted Hash Map. This will be used by the SaaS SDK to put the file in the good `bucket / 'folder'`.

```json
{
    "Name": {"S": "tenant-1"},
    "RootS3Bucket": {"S": "403993201276-tenant-group-1"}, 
    "Region": {"S": "us-west-2"}, 
    "Status": {"S": "ACTIVE"}, 
    "BasePrefix": {"S": "tenant-1/"}, 
    "GroupName": {"S": "tenant-group-1"},
    "ProcessorURL": {"S": "https://"}
}    
```

### SQS only

The following approach illustrates the simplest asynchronous architecture, using different S3 event notifications based on prefixes and one queue per tenant.

![](./diagrams/s3-sqs-fanout.drawio.png)

**Figure 5: S3 event notification to SQS tenant queue**

* There will be a limit of 100 S3 event notifications per bucket. This seems to be a hard limit. Having 1000 bucket per account per region with 100 event notifications, should support around 100k tenants.
* Using event notification definition, we can fan-out at the level of the event notification definition, one SQS queue per tenant. Below is a code sample to create the S3 event notification to target a SQS queue, and dedicated per tenant via the prefix name:

    ```python
    def eventNotificationPerTenant(tenantName,bucketName,queueArn):
    response = s3.put_bucket_notification_configuration(
        Bucket=bucketName,
        NotificationConfiguration= {
            'QueueConfigurations': [
            {
                'QueueArn': queueArn,
                'Events': [
                    's3:ObjectCreated:*'|'s3:ObjectRemoved:*'|'s3:ObjectRestore:*'| 's3:Replication:*'
                ],
                'Filter': {
                    'Key': {
                        'FilterRules': [
                            {
                                'Name': 'prefix',
                                'Value': tenantName + "/raw"
                            },
                        ]
                    }
                }
            },
        ]})
    ```

* The queue ARN will be the queue per tenant.
* This approach will scale at the level of the number of event-notification that could be created per bucket.
* The negatives of this approach is the big number of queues and event-notification to be created when we need to scale at hundred of thousand tenants. The filtering on the file to process will be done by the `event-driven transform` process.

* There is no real limit to the number of SQS queue per region per account.
* Standard queues support a maximum of 120,000 inflight messages (received from the queue but not yet deleted by the consumer). 
* On [SQS Standard queue](https://aws.amazon.com/sqs/features/) nearly unlimited number of transactions per second per API action.
* SQS is using at least once delivery.

### S3 to Lambda to SQS

The more flexible implementation may use Lambda function as a target to S3 Event Notification, to support a more flexible routing implementation, the fan-out pattern, and support batching the events. If the event-driven processing is exposed via HTTP the Lambda function may directly call the good HTTP endpoint:

![](./diagrams/s3-lambda-fanout.drawio.png){ width=750 }

**Figure 6:**

Lambda can scale at thousand of instances in parallel. The solution uses one queue per bucket and one S3 event notification definition.

As an alternate, if the event-driven processing needs to be asynchronous, then we can add queue before each event-driven process:

![](./diagrams/s3-lambda-sqs-fanout.drawio.png){ width=750 }

**Figure 7:**

The Lambda function can be a target of the S3 event notification as illustrated below:

```python
def eventNotificationPerTenantViaLambda(tenantName,bucketName,functionArn):
    response = s3.put_bucket_notification_configuration(
        Bucket=bucketName,
        NotificationConfiguration= {
          'LambdaFunctionConfigurations': [
            {
                'LambdaFunctionArn': functionArn,
                'Events': [
                     's3:ObjectCreated:*'|'s3:ObjectRemoved:*'|'s3:ObjectRestore:*'| 's3:Replication:*'|'s3:LifecycleTransition'|'s3:IntelligentTiering'|'s3:ObjectAcl:Put'|'s3:LifecycleExpiration:*'|'s3:LifecycleExpiration:Delete'|'s3:LifecycleExpiration:DeleteMarkerCreated'|'s3:ObjectTagging:*',
                 ],
                'Filter': {
                    'Key': {
                        'FilterRules': [
                            {
                               'Name': 'prefix',
                                'Value': tenantName + "/raw"
                            },
                        ]
                    }
                }
            },
            ]
        },
        )
```

Obviously we can bypass the front-end queue, and all the SQS queues if we want to be more synchronous. Except that the S3 Event notification will be posted to an internal queue in the Lambda service. But this queue is transparent to the developer.

For the Lambda routing implementation, Lambda will automatically deletes the message from the queue if it returns a success status.

The pricing model is pay per call, duration and memory used.

### SNS - SQS

In many Fanout use cases, one of the solution is to combine SNS with SQS. The S3 event notification target is now a SNS topic.

![](./diagrams/s3-sns-sqs-fanout.drawio.png)

The SNS filtering defines the target SQS queue. The event-driven processes get their tenant based workload.

### SQS - Event Bridge

Event Bridge could be a solution if the file processing is exposed with HTTP endpoint. Routing rule will be used to select the target end-point depending of the tenant information. But there is a limit of 2000 rules per event buses, so it may not be a valid solution to address the use case of thousand of tenants.

![](./diagrams/s3-sqs-eb-fanout.drawio.png)


### SQS - MSK

Finally Kafka may be a solution to stream the S3 event notification to it, via a queue. The interest will be to be able to replay the events.

![](./diagrams/s3-sqs-msk-fanout.drawio.png)

It may be more complex to deploy as we need to define a MSK cluster, and Kafka Connect from SQS source connector. The Event-driven processes need to consume from Kafka. Event ordering will be kept. It will scale to hundred of consumers.

## Demonstration script

The demonstration is based on the following combined elements:

### Prerequisites

The following steps illustrate a tenant onboarding process and the files event-driven processing. Most of the code is in Python CDK or SDK so start a virtual environment:

```sh
python3 -m venv venv
. venv/bin/activate
pip install -r requirements.txt
```

### Demonstration

1. Define environment variables: `export AWS_ACCOUNT_ID=01234567890` and `export AWS_DEFAULT_REGION=us-west-2`
1. Create a group of tenants named `tenant-group-1`, 

    ```sh
    python createGroupTenant.py -g tenant-group-1
    ```

    which will create S3 bucket, a SQS queue, the S3 event notification to target the new queue, the SQS resource policy to authorize S3 to send message to the queue and define a new tenant group record in DynamoDB TenantGroups table.

    ![](./diagrams/tenant-group.drawio.png)

1. Add a tenant, belonging to the `tenant-group-1`, which will create the following elements

    ```sh
    python createTenant.py -g tenant-group-1 -n tenant-1
    ```

    
    ![](./diagrams/tenant-elements.drawio.png)

    which will create:

    * two prefixes to persist file for the tenant: tenant-1/raw/ and tenant-1/silver/.
    * A SQS queue to receive the S3 event after routing
    * The Lambda end-point for file processing
    * The SQS policy to authorize the Lambda function to send message to the new SQS queue, and read them.
    * Record the new tenant metadata in `Tenants` table in the DynamoDB


1. Deploy the Lambda for routing to consume event from the SQS queue. The end to end solution looks like in the following figure:

    ![](./diagrams/demo-1-e2e.drawio.png)

### Cleanup

## Conclusion


## Sources of information

* [Product documentation - Amazon S3 Event Notifications](https://docs.aws.amazon.com/AmazonS3/latest/userguide/EventNotifications.html)
* [Manage event ordering and duplicate events with Amazon S3 Event Notifications - blog](https://aws.amazon.com/blogs/storage/manage-event-ordering-and-duplicate-events-with-amazon-s3-event-notifications/)
* [Getting visibility into storage usage in multi-tenant Amazon S3 buckets](https://aws.amazon.com/blogs/storage/getting-visibility-into-storage-usage-in-multi-tenant-amazon-s3-buckets/)