import boto3,os,datetime,json

TENANT_GROUP_NAME="tenant-group-1"
TENANT_GROUP_TABLE_NAME="TenantGroups"
TENANTS_TABLE_NAME="Tenants"
ACCOUNT = os.environ.get('AWS_ACCOUNT_ID')
REGION = os.environ.get('AWS_DEFAULT_REGION')

s3 = boto3.client('s3')
dynamodb = boto3.client('dynamodb')

def loadTenantGroupInformation(keyName):
    tg = dynamodb.get_item(TableName=TENANT_GROUP_TABLE_NAME, Key={'GroupName': {'S': keyName}})   
    return tg['Item']

def loadTenant(keyName):
    tg = dynamodb.get_item(TableName=TENANTS_TABLE_NAME, Key={'Name': {'S': keyName}})   
    return tg['Item']

"""
Create a tenant belonging to a group. From now the demo is scoped per region.
"""   
def createTenant(tenantName,tenantGroupInfo):
    creationDate = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    tenant = {'Name': {'S': tenantName},
              'GroupName': tenantGroupInfo['GroupName'], 
              'RootS3Bucket':  tenantGroupInfo['BucketName'],
              'BasePrefix': {'S': tenantName + "/"},
              'Region':  tenantGroupInfo['Region'], 
              'Status': {'S': 'ACTIVE' }, 
              'Created-at': {'S': creationDate }, 
              'Updated-at': {'S': creationDate }  
             }
    persistToDatabase(tenant)
    return tenant


def persistToDatabase(tenant):
    print(tenant)
    try:
        dynamodb.describe_table(TableName=TENANTS_TABLE_NAME)
        print("Table already exists")
    except:
        print("Table not found")
        dynamodb.create_table(
            AttributeDefinitions=[
                {
                    'AttributeName': 'Name',
                    'AttributeType': 'S'
                }
            ],
            TableName=TENANTS_TABLE_NAME,
            KeySchema=[
                {
                    'AttributeName': 'Name',
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
        dynamodb.get_waiter('table_exists').wait(TableName=TENANTS_TABLE_NAME)
        # Print out some data about the table.
        response = dynamodb.describe_table(TableName=TENANTS_TABLE_NAME)
        print(response)


    dynamodb.put_item(TableName=TENANTS_TABLE_NAME,
                    Item=tenant)
    


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


def eventNotificationPerTenantViaSQS(tenantName,bucketName,queueArn):
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
                                'Value': tenantName + "/"
                            },
                        ]
                    }
                }
            },
            ]
        },
        )
    print(response)


# Create a new tenant within a given group: create a prefix under the bucket for the group of tenants
# 
if __name__ == '__main__':
    #tenantGroup=loadTenantGroupInformation(TENANT_GROUP_NAME)
    #tenant=createTenant("tenant-1",tenantGroup)
    tenant=loadTenant("tenant-1")
    print(json.dumps(tenant))
