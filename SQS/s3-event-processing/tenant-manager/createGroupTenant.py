import boto3,os,datetime,json
import sys,getopt

TENANT_GROUP_NAME="tenant-group-1"
TENANT_GROUP_TABLE_NAME="TenantGroups"
ACCOUNT = os.environ.get('AWS_ACCOUNT_ID')
REGION = os.environ.get('AWS_DEFAULT_REGION',"us-west-2")

s3 = boto3.client('s3')

# create s3 bucket if not exist
def defineS3bucketForTenantGroup(account,bucketName,region):
    uniqueName=account+ "-" +bucketName
    try:
        bucket = s3.head_bucket(Bucket=uniqueName,ExpectedBucketOwner=account)
        print("Bucket already exists")
        location="http://" + uniqueName + ".s3.amazonaws.com/"
    except:
        print("Bucket not found")
        bucket = s3.create_bucket(Bucket=uniqueName,
                    CreateBucketConfiguration={
                        'LocationConstraint': region},
                    ObjectLockEnabledForBucket=False)
        location = bucket['Location']
    return uniqueName,location

   
def persistTenantGroup(groupName, bucketName, region, location, queueURL, queueArn):
    creationDate = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    groupTenant = {'GroupName': {'S': groupName}, 
              'BucketName': {'S': bucketName },
              'Region': {'S': region }, 
              'Location': {'S': location }, 
              'QueueURL': { 'S': queueURL },
              'QueueArn': {'S': queueArn }, 
              'Status': {'S': 'ACTIVE' }, 
              'Created-at': {'S': creationDate }, 
              'Updated-at': {'S': creationDate }  
              }
    persistToDatabase(groupTenant)
    return groupTenant


'''
Persist to the Dynamodb table, and create the table if it does not exist.
'''
def persistToDatabase(tenantGroup):
    client = boto3.client('dynamodb')
    # create dynamodb table if not exists
    try:
        client.describe_table(TableName=TENANT_GROUP_TABLE_NAME)
        print("Table already exists")
    except:
        print("Table not found")
        client.create_table(
            AttributeDefinitions=[
                {
                    'AttributeName': 'GroupName',
                    'AttributeType': 'S'
                }
            ],
            TableName=TENANT_GROUP_TABLE_NAME,
            KeySchema=[
                {
                    'AttributeName': 'GroupName',
                    'KeyType': 'HASH'
                }
            ],
            ProvisionedThroughput={
                'ReadCapacityUnits': 1,
                'WriteCapacityUnits': 1
            }
        )
        print("Table created")
        # Wait until the table exists.
        client.get_waiter('table_exists').wait(TableName=TENANT_GROUP_TABLE_NAME)
        # Print out some data about the table.
        response = client.describe_table(TableName=TENANT_GROUP_TABLE_NAME)
        print(response)


    client.put_item(TableName=TENANT_GROUP_TABLE_NAME,
                    Item=tenantGroup)
    

'''
Add a S3 event notification to target the queue if it does not exist.
'''
def addEventNotificationToQueue(bucketName,queueArn):
    print("Adding event notification to " + bucketName + " bucket, with target queue: " + queueArn)
    response=s3.get_bucket_notification_configuration(Bucket=bucketName)
    found=False
    print(json.dumps(response,indent=3))
    if 'QueueConfigurations' in response:
        for config in response['QueueConfigurations']:
            if config['QueueArn'] == queueArn:
                print("Bucket already has event notification for this queue")
                found=True
    if not found:
        print("Bucket does not have event notification")
        s3.put_bucket_notification_configuration(
                    Bucket=bucketName,
                    NotificationConfiguration={
                        'QueueConfigurations': [
                            {
                                'QueueArn': queueArn,
                                'Events': [
                                    's3:ObjectCreated:*','s3:ObjectRemoved:*'
                                ],
                            }
                        ]
                    },)



# create sqs queue if does not exist, returns queue URL and queue ARN
def defineTenantGroupQueue(queueName,bucketName):
    sqs = boto3.client('sqs')
    try:
        response = sqs.get_queue_url(
                    QueueName=queueName,
                    QueueOwnerAWSAccountId=ACCOUNT
                )
        print("Queue exists")
    except sqs.exceptions.QueueDoesNotExist:
        print("Queue not found")
        response = createQueue(sqs,queueName)
    print(json.dumps(response,indent=3))
    queueURL = response['QueueUrl']
    queueArn = sqs.get_queue_attributes(
                        QueueUrl=queueURL,
                        AttributeNames=['QueueArn'])['Attributes']['QueueArn']
    return queueURL,queueArn


def createQueue(sqs,queueName):
    response = sqs.create_queue(
                    QueueName=queueName,
                    Attributes={
                        'DelaySeconds': '60',
                        'MessageRetentionPeriod': '86400'
                    }
                )
    queueURL = response['QueueUrl']
    queueArn = sqs.get_queue_attributes(
                        QueueUrl=queueURL,
                        AttributeNames=['QueueArn'])['Attributes']['QueueArn']
    policy = {
        "Version": "2012-10-17",
        "Statement": [
            {
                "Sid": "allow-s3",
                "Effect": "Allow",
                "Action": "sqs:SendMessage",
                "Principal": {
                     "Service": "s3.amazonaws.com"
                },
                "Resource": queueArn,
            }
        ]
    }

    
    sqs.set_queue_attributes(
        QueueUrl=queueURL,
        Attributes={
            'Policy': json.dumps(policy)
        }
    )
    return response
    



def usage():
    print("Usage: python createGroupTenant.py [-h | --help] [ -g tenant-group-name ]")
    print("Example: python createGroupTenant.py tenant-group-1")
    sys.exit(1)

def processArguments():
    try:
        opts, args = getopt.getopt(sys.argv[1:], "h:g:", ["help","tenant_group"])
    except getopt.GetoptError as err:
        usage()
    
    for opt, arg in opts:
        if opt in ("-h", "--help"):
            usage()
        elif opt in ("-g", "--tenant_group"):
            TENANT_GROUP_NAME = arg
    return TENANT_GROUP_NAME

# Create a new tenant group, with one matching SQS queue
# 
if __name__ == '__main__':
    TENANT_GROUP_NAME=processArguments()
    print("---- Creation of the infrastructure for the solution in " + ACCOUNT + " for a group of tenant " + TENANT_GROUP_NAME)
    bucketName,location=defineS3bucketForTenantGroup(ACCOUNT,TENANT_GROUP_NAME,REGION)

    queueURL,queueArn=defineTenantGroupQueue(TENANT_GROUP_NAME,bucketName)

    addEventNotificationToQueue(bucketName,queueArn)
    tenantGroup=persistTenantGroup(TENANT_GROUP_NAME,bucketName,REGION,location,queueURL,queueArn)
    print(json.dumps(tenantGroup, indent=3))
    
