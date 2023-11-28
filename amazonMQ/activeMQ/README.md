# Active MQ examples

This folder includes a set of Active MQ reference code, with Infrastructure as code to deploy on AWS.

As Active MQ has two main different product version the classic and artemis folder have different code and docker compose files.

Updated 11/27/2023 - tested

## Classic

The classic has request-replyTo queues between one application, orchestrator of an order entity and one participant to the business process of managing this order. This is the Command pattern.

### Happy path


### Failover demo

The docker compose, in the `failover` folder, uses the latest apache activemq image, in active/passive mode with the two applications within `request-replyto` folder.

See [demo script](https://jbcodeforce.github.io/aws-messaging-study/labs/failover-lab/)