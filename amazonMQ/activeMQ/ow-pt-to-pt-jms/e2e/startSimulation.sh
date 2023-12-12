curl -X 'POST' \
    'http://localhost:8081/carrides/simulator' \
    -H 'accept: */*' \
    -H 'Content-Type: application/json' \
    -d '{
    "totalMessageToSend": 10
    }'