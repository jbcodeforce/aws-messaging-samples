import json, boto3 


s3 = boto3.client('s3')

def processRecords(records):
    for record in records:
        bucket = record["s3"]["bucket"]
        objectName=record["s3"]["object"]["key"]
        processS3File(bucket,objectName)
        


def processS3File(bucketName,objectName):
    print("Process " + bucketName+"/"+objectName)
    s3.download_file(bucketName, objectName, objectName)

    s3.upload_file('my-local-file', 'my-bucket', 'my-key')


# open a json file
with open('out.json', 'r') as f:
    events= json.load(f)
    processRecords(events["Records"])


