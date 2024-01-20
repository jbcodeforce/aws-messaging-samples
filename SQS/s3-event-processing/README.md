# S3 Event Notification with Lambda and SQS

The presentation of the challenge addressed in this proof of concept is done in [this note](https://jbcodeforce.github.io/aws-messaging-study/labs/sqs/s3-tenants-async-processing/). The solution to support the demonstration is illustrated in the following figure:

![architecture](https://github.com/jbcodeforce/aws-messaging-study/blob/main/docs/labs/sqs/diagrams/demo-1-e2e.drawio.png)

## Prerequisites

* You should have AWS account, AWS ClI installed, Python 3 environment, AWS SAM, CDK CLI installed on your computer.
* Initiate a Python virtual environment under the `aws-messaging-study/SQS/s3-event-processing` folder.

```sh
python3 -m venv venv
.venv/bin/activate
pip install -r requirements.txt
```

* Define environment variables: 

```
export AWS_ACCOUNT_ID=01234567890
export AWS_DEFAULT_REGION=us-west-2
```

## Create Infrastructure with SDK

This section demonstrate how a tenant manager application may be able to create element of the solution using SDK.

1. Go under `tenant-manager` folder.
1. Create a group of tenants named `tenant-group-1`

    ```sh
    python createGroupTenant.py -g tenant-group-1
    ```

    which creates S3 bucket, a SQS queue to get events from the bucket, the S3 event notification to target the new queue, the SQS resource policy to authorize S3 to send message to the queue and defines a new tenant group record in DynamoDB TenantGroups table.

    ![](https://github.com/jbcodeforce/aws-messaging-study/blob/main/docs/labs/sqs/diagrams/tenant-group.drawio.png)

    Example of output:

    ```json
    {
        "GroupName": {
            "S": "tenant-group-1"
        },
        "BucketName": {
            "S": "ACCOUNT_ID-tenant-group-1"
        },
        "Region": {
            "S": "http://ACCOUNT_ID-tenant-group-1.s3.amazonaws.com/"
        },
        "QueueURL": {
            "S": "https://sqs.us-west-2.amazonaws.com/ACCOUNT_ID/tenant-group-1"
        },
        "QueueArn": {
            "S": "arn:aws:sqs:us-west-2:ACCOUNT_ID:tenant-group-1"
        },
        "Status": {
            "S": "ACTIVE"
        },
    ```

1. Add a tenant, belonging to the `tenant-group-1`

    ```sh
    python createTenant.py -g tenant-group-1 -n tenant-1
    ```

    which will create the following elements
    
    ![](https://github.com/jbcodeforce/aws-messaging-study/blob/main/docs/labs/sqs/diagrams/tenant-elements.drawio.png)

    * two prefixes to persist file for the tenant: tenant-1/raw/ and tenant-1/silver/.
    * A SQS queue to receive the S3 event after routing
    * The Lambda end-point for file processing
    * The SQS policy to authorize the Lambda function to send message to the new SQS queue, and read them.
    * Record the new tenant metadata in `Tenants` table in the DynamoDB

    ```json
    {
        "Name": {
            "S": "tenant-1"
        },
        "GroupName": {
            "S": "tenant-group-1"
        },
        "RootS3Bucket": {
            "S": "ACCOUNT_ID-tenant-group-1"
        },
        "BasePrefix": {
            "S": "tenant-1/"
        },
        "Region": {
            "S": "http://ACCOUNT_ID-tenant-group-1.s3.amazonaws.com/"
        },
        "Status": {
            "S": "ACTIVE"
        },
        "TargetQueueForRawFile": {
            "S" : "https://sqs.REGION.amazonaws.com/ACCOUNT_ID/tenant-1-raw"
        }

    ```
1. Add 2 other tenants:

    ```sh
    python createTenant.py -g tenant-group-1 -n tenant-2
    python createTenant.py -g tenant-group-1 -n tenant-3
    ```

1. Deploy the Lambda for tenant routing to consume events from the first SQS queue and route to the target SQS queue. For that, use the sam cli under the `s3_event_processing` folder.

    ```sh
    sam build
    sam validate
    sam deploy --guided
    ```

    Here is the resources created via CloudFormation stack:

    ![](https://github.com/jbcodeforce/aws-messaging-study/blob/main/docs/labs/sqs/images/cf-lambdap-dep.png)

    and the deployed function:

    ![](https://github.com/jbcodeforce/aws-messaging-study/blob/main/docs/labs/sqs/images/1-lambda-evt-p.png)

    Any modification of the Lambda code can be redeployed using `sam build && sam ydeploy`.

## Demonstrate end to end processing

1. Write a source file to a tenant raw destination, as it is in the tenant-1-group bucket the S3 Event notification will be propagated to the first SQS queue, the deploy Lambda function will route to the good tenant SQS queue:

    ```sh
    python writeRawData.py -g tenant-group-1 -n tenant-1
    python writeRawData.py -g tenant-group-1 -n tenant-2
    python writeRawData.py -g tenant-group-1 -n tenant-3
    ```

    The Lambda function is processing the events:

    ![](https://github.com/jbcodeforce/aws-messaging-study/blob/main/docs/labs/sqs/images/lambda-monitoring.png)


## Clean up

    ```
    sam destroy
    ```
