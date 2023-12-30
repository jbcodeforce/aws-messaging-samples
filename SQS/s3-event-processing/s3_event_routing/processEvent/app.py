import json
import boto3

TENANTS_TABLE_NAME="Tenants"

def lambda_handler(event, context):
    for message in event['Records']:
        process_message(message)
    return {
        "statusCode": 200,
        "body": json.dumps({
            "message": "Done processing S3 Event Notification"
        }),
    }


def process_message(message):
    try:
        s3Notification=message['s3']
        print(f"Processed message {s3Notification}")
        bucketName=s3Notification['bucket']['name']
        print(f"Bucket Name: {bucketName}")
        objectKey=s3Notification['object']['key']
        if "/raw/" in objectKey:
            tenantName=objectKey.split('/')[0]
            sequencer=s3Notification['object']['sequencer']
            processEventFromRawFolder(tenantName,sequencer)    
        else:
            print("no processing")

    except Exception as err:
        print("An error occurred")
        raise err

def processEventFromRawFolder(tenantName,sequencer):
    """ Process Event from Raw Folder """
    print("Processing Event from Raw Folder")
    tenantInfo=lookupTenant(tenantName)
    targetQueueForRawFile = tenantInfo['Item']['TargetQueueForRawFile']['S']
    print(tenantInfo)

def lookupTenant(tenantName):
    """ Lookup Tenant in DynamoDB """
    dynamodb = boto3.resource('dynamodb')
    table = dynamodb.Table(TENANTS_TABLE_NAME)
    response = table.get_item(
        Key={
            'Name': tenantName
        }
    )
    return response

def s3_notif_event():
    """ Generates S3 Event Notification"""

    return {
         "Records": [ { "s3": {
                "s3SchemaVersion": "1.0",
                "configurationId": "YjE1ZDFmNDgtNDdkYy00YTg4LTkwMmUtMDJhNzBlZjc0OGI0",
                "bucket": {
                    "name": "tenant-group-1",
                    "ownerIdentity": {
                        "principalId": "ASZSZNK23IX0X"
                    },
                    "arn": "arn:aws:s3:::tenant-group-1"
                },
                "object": {
                    "key": "tenant-1/raw/tenant.json",
                    "size": 84,
                    "eTag": "b9dd11ce5b653aaafd256ec6e742bc13",
                    "sequencer": "00658F76CC18638712"
                } 
         }
        }]
    }

lambda_handler(s3_notif_event(), None)