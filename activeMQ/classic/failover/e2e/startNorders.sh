#!/bin/bash
if [ $# -eq 1 ]
then
    count=$1
else
    count=10
fi
# concat a string
data='{"delay": 0, "totalMessageToSend": '${count}'}'
echo ${data}

curl -X POST 'http://localhost:8081/orders/simulation' -H 'accept: application/json' -H 'Content-Type: application/json' -d "$data"