# Point to Point JMS based producer and consumer

The code is under the [activeMQ/pt-to-pt]() folder.

## Requirements

* Produce message to a queue using JMS protocol, using Quarkus constructs.
* Consumer in a separate app, using JMS

## Running Locally

* Build the two docker images for jms-producer and jms-consumer components

```sh
# under jms-producer
./buildAll
# jms-consumer
./buildAll 
```

* Start the one ActiveMQ artemis broker with the the producer and consumer apps:

```sh
docker compose -f jms-docker-compose.yml up -d
```

* The Active MQ console: [http://localhost:8161/console](http://localhost:8161/).
* Use the Producer REST API to send n messages. [Producer API](http://localhost:8081/q/swagger-ui), the consumer should get the messages in the logs (`docker logs consumer`).

## Demonstration scripts

## Code Explanation

## Deploy on AWS

### Using AWS CLI

The `createBrokers.sh` script creates brockers using AWS CLI, and the [CLI product document](https://awscli.amazonaws.com/v2/documentation/api/latest/reference/mq/index.html) for parameter details.