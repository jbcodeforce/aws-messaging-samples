import boto3,os,datetime,json

GROUP_BUCKET_NAME="tenant-group-1"
ACCOUNT = os.environ.get('AWS_ACCOUNT_ID')
REGION = os.environ.get('AWS_DEFAULT_REGION')

s3 = boto3.client('s3')

# create s3 bucket
def defineTenantGroup(bucketName):
    uniqueName=ACCOUNT+ "-" +bucketName
    print(uniqueName)
    try:
        bucket = s3.head_bucket(Bucket=uniqueName,ExpectedBucketOwner=ACCOUNT)
    except:
        print("Bucket not found")
        bucket = s3.create_bucket(Bucket=uniqueName,
                    CreateBucketConfiguration={
                        'LocationConstraint': REGION},
                    ObjectLockEnabledForBucket=False)
    return bucket['x-amz-bucket-region']

   
def createTenant(tenantName,bucketLocation):
    creationDate = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    tenant = {'name': tenantName, 
              'group': GROUP_BUCKET_NAME, 
              'bucket': bucketLocation,
              'base-prefix': tenantName + "/",
              'region': REGION, 
              'status': 'ACTIVE', 
              'created-at': creationDate, 'updated-at': creationDate }
    persistToDatabase(tenant)
    return tenant


def persistToDatabase(tenant):
    pass


def eventNotificationToQueue(bucketName,queueArn):
    
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


def eventNotificationPerTenant(tenantName,bucketName,queueArn):
    response = s3.put_bucket_notification_configuration(
        Bucket=bucketName,
        NotificationConfiguration= {
            'QueueConfigurations': [
            {
                'QueueArn': queueArn,
                'Events': [
                    's3:ObjectCreated:*'|'s3:ObjectRemoved:*'|'s3:ObjectRestore:*'| 's3:Replication:*'|'s3:LifecycleTransition'|'s3:IntelligentTiering'|'s3:ObjectAcl:Put'|'s3:LifecycleExpiration:*'|'s3:LifecycleExpiration:Delete'|'s3:LifecycleExpiration:DeleteMarkerCreated'|'s3:ObjectTagging:*',
                ],
                'Filter': {
                    'Key': {
                        'FilterRules': [
                            {
                                'Name': 'prefix',
                                'Value': tenantName + "/"
                            },
                        ]
                    }
                }
            },
        ]})
    print(response)

# create sqs queue
def defineTenantGroupQueue(queueName):
    sqs = boto3.resource('sqs')
    queue = sqs.create_queue(QueueName=queueName)
    print(queue)
    #print(queue.attributes.get('DelaySeconds'))


# Create a new tenant within a given group: create a prefix under the bucket for the group of tenants
# 
if __name__ == '__main__':
    bucketLocation=defineTenantGroup(GROUP_BUCKET_NAME)
    tenant=createTenant("tenant-1",bucketLocation)
    print(json.dumps(tenant))
    #defineTenantGroupQueue(GROUP_BUCKET_NAME)