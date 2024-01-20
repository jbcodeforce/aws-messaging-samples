import json
import boto3, logging

TENANTS_TABLE_NAME="Tenants"

def lambda_handler(event, context):
    
    for message in event['Records']:
        logging.info(json.dumps(message,indent=3))
        process_message(message)
    return {
        "statusCode": 200,
        "body": json.dumps({
            "message": "Done processing S3 Event Notification"
        }),
    }

'''
Process S3 event notification.
'''
def process_message(message):
    try:
        if 's3' in message:
            s3Notification=message['s3']
            bucketName=s3Notification['bucket']['name']
            logging.info(f"Tenant Group Bucket Name: {bucketName}")
            objectKey=s3Notification['object']['key']
            if "/raw/" in objectKey:
                tenantName=objectKey.split('/')[0]
                sequencer=s3Notification['object']['sequencer']
                processEventFromRawFolder(tenantName,sequencer,objectKey)    
            else:
                logging.info("no processing")
        else:
            logging.error("No S3 element in the S3 event notification record")
            logging.error(message)
    except Exception as err:
        logging.error("An error occurred")
        raise err

'''
When file in raw prefix, delegate to the downstream processing by sending to the tenant queue
'''
def processEventFromRawFolder(tenantName,sequencer,fileName):
    """ Process Event from Raw Folder """
    logging.info("Processing Event from Raw Folder")
    tenantInfo=lookupTenant(tenantName)
    logging.info("--> tenant info from database: \n" + json.dumps(tenantInfo,indent=3))
    queueUrl = tenantInfo['Item']['TargetQueueForRawFile']
    message = {"FileToProcess": fileName }
    sendMessageToDestinationQueue(queueUrl,json.dumps(message),sequencer)


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

def sendMessageToDestinationQueue(queueURL,message,sequencer):
    sqs = boto3.client('sqs')
    response = sqs.send_message(QueueUrl=queueURL,MessageBody=message)
    logging.info(json.dumps(response,indent=3))



