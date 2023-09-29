# JMS based request-replyTo demonstration


The code is under [jms-orchestrator](./activeMQ/jms-orchestrator/) and [jms-participant](./activeMQ/jms-participant), to implement a request-response over queue using JMS. This code use client acknowledgement, and replyTo queue. It does not use CDI for bean injection but code base instantiation of the ConnectionFactor. The docker compose also define a Active / Standby Active MQ nodes. 

## Requirements

* Expose GET, POST, PUT `/orders` api
* Mockup a repository in memory
* On POST or PUT operations, order messages are sent to another service (the participant) to act on them via a `orders` queue, and get the response to `orders-reply` queue.
* Support once and only once semantic
* Expose a POST /orders/simulation to run n order creation with random data, to support a failover demonstration.

## Running Locally

While in development mode:

1. Start Active MQ: ` docker compose -f artemis-docker-compose.yml up -d`
1. Start each application with `quarkus dev`
1. Orchestrator Application URL: [http://localhost:8081/](http://localhost:8081/) and swagger-ui
1. See ActiveMQ console: [http://localhost:8161/](http://localhost:8161/), admin/adminpassw0rd

## Demonstration scripts

1. Post new order: 

    ```sh
    curl -X 'POST' 'http://localhost:8081/orders' -H 'accept: application/json' -H 'Content-Type: application/json' -d@./e2e/neworder.json
    ```

## Code Explanation

## Deploy on AWS