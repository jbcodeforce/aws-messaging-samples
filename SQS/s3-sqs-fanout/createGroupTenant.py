import boto3,os,datetime,json

TENANT_GROUP_NAME="tenant-group-1"
TENANT_GROUP_TABLE_NAME="TenantGroups"
ACCOUNT = os.environ.get('AWS_ACCOUNT_ID')
REGION = os.environ.get('AWS_DEFAULT_REGION')

s3 = boto3.client('s3')

# create s3 bucket
def defineTenantGroup(bucketName):
    uniqueName=ACCOUNT+ "-" +bucketName
    try:
        bucket = s3.head_bucket(Bucket=uniqueName,ExpectedBucketOwner=ACCOUNT)
    except:
        print("Bucket not found")
        bucket = s3.create_bucket(Bucket=uniqueName,
                    CreateBucketConfiguration={
                        'LocationConstraint': REGION},
                    ObjectLockEnabledForBucket=False)
    return uniqueName,bucket['ResponseMetadata']['HTTPHeaders']['x-amz-bucket-region']

   
def persistTenantGroup(groupName, bucketName, location, queueURL, queueArn):
    creationDate = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    groupTenant = {'GroupName': {'S': groupName}, 
              'BucketName': {'S': bucketName },
              'Region': {'S': location }, 
              'QueueURL': { 'S': queueURL },
              'QueueArn': {'S': queueArn }, 
              'Status': {'S': 'ACTIVE' }, 
              'Created-at': {'S': creationDate }, 
              'Updated-at': {'S': creationDate }  
              }
    persistToDatabase(groupTenant)
    return groupTenant


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
    


def addEventNotificationToQueue(bucketName,queueArn):
    
    response = s3.put_bucket_notification_configuration(
        Bucket=bucketName,
        NotificationConfiguration= {
            'QueueConfigurations': [
            {
                'QueueArn': queueArn,
                'Events': [
                    's3:ObjectCreated:*'|'s3:ObjectRemoved:*'|'s3:ObjectRestore:*'| 's3:Replication:*'|'s3:LifecycleTransition'|'s3:IntelligentTiering'|'s3:ObjectAcl:Put'|'s3:LifecycleExpiration:*'|'s3:LifecycleExpiration:Delete'|'s3:LifecycleExpiration:DeleteMarkerCreated'|'s3:ObjectTagging:*',
                ]
            },
        ]})
    print(response)


# create sqs queue
def defineTenantGroupQueue(queueName):
    sqs = boto3.client('sqs')
    try:

        response = sqs.get_queue_url(
                    QueueName=queueName,
                    QueueOwnerAWSAccountId=ACCOUNT
                )
    except sqs.exceptions.QueueDoesNotExist:
        print("Queue not found")
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
                        AttributeNames=['QueueArn'])
    return queueURL,queueArn['Attributes']['QueueArn']



# Create a new tenant within a given group: create a prefix under the bucket for the group of tenants
# 
if __name__ == '__main__':
    bucketName,location=defineTenantGroup(TENANT_GROUP_NAME)
    queueURL,queueArn=defineTenantGroupQueue(TENANT_GROUP_NAME)
    tenantGroup=persistTenantGroup(TENANT_GROUP_NAME,bucketName,location,queueURL,queueArn)
    print(json.dumps(tenantGroup, indent=3))
    #addEventNotificationToQueue(bucketName,queueArn)
