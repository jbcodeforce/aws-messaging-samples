import json

import pytest

from processEvent import app


@pytest.fixture()
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


def test_lambda_handler(s3_notif_event):

    ret = app.lambda_handler(s3_notif_event, "")
    data = json.loads(ret["body"])

    assert ret["statusCode"] == 200
    assert "message" in ret["body"]
    assert data["message"] == "Done processing S3 Event Notification"
