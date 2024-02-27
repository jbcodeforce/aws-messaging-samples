import json

import pytest

from s3_event_routing import app


@pytest.fixture()
def sns_event():
    """ Generates SNS Event"""
    msg =json.load(open('../events/event.json'))
    return msg


def test_lambda_handler(sns_event):

    ret = app.lambda_handler(sns_event, "")
    
