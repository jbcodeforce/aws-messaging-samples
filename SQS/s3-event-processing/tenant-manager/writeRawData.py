import boto3,json,getopt,sys,os,datetime

ACCOUNT = os.environ.get('AWS_ACCOUNT_ID')
REGION = os.environ.get('AWS_DEFAULT_REGION')

def usage():
    print("Usage: python writeRawData.py [-h | --help] [ -n tenant-name ]")
    print("Example: python writeRawData.py-n tenant-1")
    sys.exit(1)

def processArguments():
    try:
        opts, args = getopt.getopt(sys.argv[1:], "h:g:n:", ["help","tenant_group","tenant_name"])
    except getopt.GetoptError as err:
        usage()
    
    for opt, arg in opts:
        if opt in ("-h", "--help"):
            usage()
        elif opt in ("-g", "--tenant_group"):
            TENANT_GROUP_NAME = arg
        elif opt in ("-n", "--tenant_name"):
            TENANT_NAME = arg
    return TENANT_GROUP_NAME,TENANT_NAME


def writeToTenantPrefix(account,tenant_group,tenantName):
    s3 = boto3.client('s3')
    tenantInfo = { "name": tenantName,
                   "group": tenant_group,
                   "created-at":  datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")}
    #bucketName=account+ "-" + tenant_group
    bucketName=tenant_group
    s3.put_object(Body=json.dumps(tenantInfo), Bucket=bucketName, Key=tenantName + "/raw/" + "tenant.json")
    print("Tenant information written to " + bucketName + "/" + tenantName + "/raw/" + "tenant.json")
    


if __name__ == '__main__':
    TENANT_GROUP_NAME,TENANT_NAME = processArguments()
    writeToTenantPrefix(ACCOUNT,TENANT_GROUP_NAME,TENANT_NAME)
