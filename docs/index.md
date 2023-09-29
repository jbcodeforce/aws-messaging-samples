# Introduction

This repository includes sample application code, IaC to create Amazon MQ with different engine types. The AWS MQ workshops have all we need to start learning Amazon MQ. 

## [Active MQ](https://activemq.apache.org/)

Active MQ is an Open Source software, multi-protocol, java based message broker. The Artemis version is supporting JMS 2.0. It supports message load balancing, HA. Multiple connected "master" brokers can dynamically respond to consumer demand by moving messages between the nodes in the background.

### AWS Samples

* [Github amazon-mq-workshop](https://github.com/aws-samples/amazon-mq-workshop/tree/master)
* [Github Amazon MQ Network of Brokers](https://github.com/aws-samples/aws-mq-network-of-brokers)

### Solutions in this repository

* [Point to Point JMS based producer and consumer](./pt-to-pt-jms.md)
* [Request-ReplyTo order ochestrator and participant based on JMS](./req-reply.md)

## [Rabbit MQ](https://www.rabbitmq.com/)

The other Open Source project.

## Interesting content to read

* [AWS Active MQ Workshop](https://catalog.us-east-1.prod.workshops.aws/workshops/0b534eb9-fdfb-49f0-8df4-ebccca71a9eb/en-US)
* [Amazon MQ RabbitMQ workshop](https://catalog.us-east-1.prod.workshops.aws/workshops/88db3818-a8bb-4f5c-acf9-e57fa7a129b6/en-US)
* [Create broker AWS CLI command.](https://awscli.amazonaws.com/v2/documentation/api/latest/reference/mq/create-broker.html)
* [Amazon MQ CLI](https://github.com/antonwierenga/amazonmq-cli).
* [Implementing enterprise integration patterns with AWS messaging services: point-to-point channels.](https://aws.amazon.com/blogs/compute/implementing-enterprise-integration-patterns-with-aws-messaging-services-point-to-point-channels/)