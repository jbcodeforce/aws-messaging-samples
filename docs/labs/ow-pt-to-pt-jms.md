# On-way pattern with Point to Point JMS based producer and consumer

The code is under the [activeMQ/classic/ow-pt-to-pt-jms](https://github.com/jbcodeforce/aws-messaging-study/tree/main/amazonMQ/activeMQ/classic/ow-pt-to-pt-jms) folder.

## Requirements

* Produce message to a queue using JMS protocol, using Quarkus constructs.
* Consumer in a separate app, using JMS

![](./diagrams/ow-p2p-amq-jms.drawio.png)

## Running Locally

* Build the two docker images for jms-producer and jms-consumer components

```sh
# under jms-producer
./buildAll.sh
# jms-consumer
./buildAll.sh 
```

Or use under the `ow-pt-to-pt-jms` folder the `./buildAll.sh` command.

* Start the one ActiveMQ artemis broker with the the producer and consumer apps:

```sh
docker compose up -d
```

* The Active MQ console: [http://localhost:8161/console](http://localhost:8161/).
* Use the Producer REST API to send one CarRide at [Producer home](http://localhost:8081/) and then the Swagger UI link, using the following json:

    ```json
    {
    "customerID": "C01",
    "pickup": "Location_1",
    "destination": "Location_2",
    "rideDate": "11/30/2023",
    "rideTime": "10:00",
    "numberOfPassengers": 2
    }
    ```

    ![](./images/ow-p2p-amq.png)


    Another way is to use curl

    ```sh
    curl -X 'POST' \
    'http://localhost:8081/carrides' \
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

* Once the consumer is running the message appears in the logs: `docker logs consumer`. Or a call to the REST URL will get the last one received:

    ```
    curl -X 'GET' \
    'http://localhost:8080/carrides/last' \
    -H 'accept: text/plain'
    ```

    See also the swagger: http://localhost:8080/q/swagger-ui/

* Finally use the Simulator controller to send n random CarRides

    ```sh
    curl -X 'POST' \
    'http://localhost:8081/carrides/simulator' \
    -H 'accept: application/json' \
    -H 'Content-Type: application/json' \
    -d '{
    "delay": 0,
    "totalMessageToSend": 10
    }'
    ```

* Stop the demo: `docker compose down`

## Code Explanation

## Deploy on AWS

* Create ECR repositories for the two app:

    ```sh
    aws ecr create-repository --repository-name j9r/amq-jms-consumer
    aws ecr create-repository --repository-name j9r/amq-jms-producer
    ```

* Push the two docker images to ECR

    ```sh
    export AWS_ACCOUNT_ID=
    aws ecr get-login-password --region us-west-2 | docker login --username AWS --password-stdin $AWS_ACCOUNT_ID.dkr.ecr.us-west-2.amazonaws.com
    docker tag j9r/amq-jms-consumer:latest $AWS_ACCOUNT_ID.dkr.ecr.us-west-2.amazonaws.com/j9r/amq-jms-consumer:latest
    docker push $AWS_ACCOUNT_ID.dkr.ecr.us-west-2.amazonaws.com/j9r/amq-jms-consumer:latest
    #
    docker tag j9r/amq-jms-producer:latest $AWS_ACCOUNT_ID.dkr.ecr.us-west-2.amazonaws.com/j9r/amq-jms-producer:latest
    docker push $AWS_ACCOUNT_ID.dkr.ecr.us-west-2.amazonaws.com/j9r/amq-jms-producer:latest

    ```

* Create ECS Cluster in an existing VPC

    ```sh
    aws ecs create-cluster --cluster-name demo-ecs
    ```

* Create Active MQ broker

### Using AWS CLI

The `createBrokers.sh` script creates brockers using AWS CLI, and the [CLI product document](https://awscli.amazonaws.com/v2/documentation/api/latest/reference/mq/index.html) for parameter details.