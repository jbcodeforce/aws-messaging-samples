# A simple Active MQ manager without JMX

This quarkus app exposes queues management REST operations:


| Path | Operation | Description | Example | Parameters |
| --- | --- | --- | --- | --- |
| /queues/ | POST | Create a new queue | { "name" : "carrides.dlq" } | a Json doc with properties for the queue |
| /queues/ | GET | Get the list of queues |  | a Json doc with queue descriptions |
| /queues/ | DELETE | Delete the specified queue | { "name" : "carrides.dlq" } | a Json doc with queue name |
| /queues/{queue_name} | GET | Browse messages in the given queue | | A list of messages with messageID, and payload as text |
| /queues/{queue_name} | POST | Post a demo CarRide object| { "customerID": "C01","pickup": "Location_1", "destination": "Location_2", "rideDate": "11/30/2023", "rideTime": "10:00",    "numberOfPassengers": 2 } | a Json Doc to define a car ride. |
| /queues/{queue_name}/moveMessageTo | PUT | Move a message from the queue as parameter to a destination, given the JMS message id | { "destinationName" : "carrides", "messageId": "..."  } | A Json doc with destination and messageId |
| /queues/{queue_name}/moveMatchingMessageTo | PUT | Move messages matching a filter from the queue as parameter to a destination | { "destinationName" : "carrides", "selector": " a sql filter" } | A Json doc with destination and filter selector |


## Running locally for development

* Start local Active MQ using docker compose:

```sh
docker compose -f dev-dc.yml up -d
```

* Start Quarkus app in dev mode:

```sh
quarkus dev
```

* Access Active MQ console at [http://localhost:8161](http://localhost:8161/index.html)
* Use the API in a browser: [http://localhost:8081/q/swagger-ui](http://localhost:8081/q/swagger-ui)
* Get existing queues:

```sh
curl -X GET localhost:8081/queues  -H 'accept: application/json'
```

* Create new queue:

```sh
curl -X 'POST' 'http://localhost:8081/queues' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '{
  "name": "carrides",
  "persistent": true
}'

# And
curl -X 'POST' 'http://localhost:8081/queues' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '{
  "name": "carrides.dlq",
  "persistent": true
}'
```


* Send a message to a queue using a json payload:

```sh
curl -X 'POST' \
  'http://localhost:8081/queues/message/carrides.dlq' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '{
    "customerID": "C01",
    "pickup": "Location_1",
    "destination": "Location_2",
    "rideDate": "11/30/2023",
    "rideTime": "10:00",
    "numberOfPassengers": 2
    }'
```

* Browse messages in queue:

```sh
curl -X 'GET' \
  'http://localhost:8081/queues/carrides.dlq' \
  -H 'accept: application/json'
```

* Move a message from DLQ to origin

```sh
curl -X 'PUT' \
  'http://localhost:8081/queues/carrides.dlq/moveMessageTo' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '{
  "messageId": "001",
  "destinationName": "carrides"
}'
```

* Clean up: stop quarkus app, and Active MQ docker container.

```sh
docker compose -f dev-dc.yml down
rm -rf data
```

## Automatic Integration tests

The `src/java/test` folder has unit and integration tests using TestContainer.

## Deploy on AWS

### Deploy a Broker for Development

### ECS Fargate deployment

