import json,boto3,logging

# import requests

TENANTS_TABLE_NAME="Tenants"
sqs = boto3.client('sqs')
dynamodb = boto3.resource('dynamodb')

def lambda_handler(event, context):
    for message in event['Records']:
        
        process_message(message)

'''
Process S3 event notification.
'''
def process_message(message):
    print(json.dumps(message,indent=3))