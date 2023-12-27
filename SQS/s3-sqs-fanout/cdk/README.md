
# A sample to demonstrate how to scale S3 event processing

This CDK stack creates an S3 bucket, allows to upload objects to that bucket, and will send notifications from S3 to SQS when an object is created in that bucket. Then it's fanout using EventBridge to event processors

## Resources created

* SQS Queue
* SQS Queue Policy
* S3 Bucket
* A Lambda function to create the S3 event notification via SDK and the matching IAM Role to let the Lambda function to put bucket notification

```json
    "Action": "s3:PutBucketNotification",
            "Resource": "*",
            "Effect": "Allow"
``` 


## Getting started

* Create a virtual environment for python:

```
$ python3 -m venv .venv
```

* Activate your virtualenv.

```
$ source .venv/bin/activate
```

If you are a Windows platform, you would activate the virtualenv like this:

```
% .venv\Scripts\activate.bat
```

*  Install python modules:

    ```bash
    python3 -m pip install -r requirements.txt
    ```

* Synthesize the CloudFormation template for this code.

```
$ cdk synth
```

See this note for demonstration script once the stack is deployed.
