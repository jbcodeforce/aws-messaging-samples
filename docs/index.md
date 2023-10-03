# Introduction

This repository includes a set of demo applications and IaC to create Amazon MQ with different engine types. 

The AWS MQ workshops contain a lot of information to get started with Amazon MQ. This repositoryy addresses some of the specific subjects not covered in detail in those workshops: JMS programming model, separation of application, different ActiveMQ release, supports running locally with docker. Unit testing, integration test, reactive messaging and reactive programming.
For the IaC, we use AWS CDK, with different stack to be able to reuse common infrastructure like VPC, IAM roles, Cloud9 environment...

## [Active MQ](https://activemq.apache.org/)

Active MQ is an Open Source software, multi-protocol, java based message broker. The Artemis version is supporting JMS 2.0. It supports message load balancing, HA. Multiple connected "master" brokers can dynamically respond to consumer demand by moving messages between the nodes in the background.


### Solutions in this repository

* [Request-ReplyTo order ochestrator and participant based on JMS - ActiveMQ Classic release](./classic-req-reply-jms.md)
* [Point to Point JMS based producer and consumer - ActiveMQ Artemis release](./pt-to-pt-jms.md)
* [Request-ReplyTo order ochestrator and participant based on JMS - ActiveMQ Artemis release](./req-reply-jms.md)
* [AMQP Quarkus app point to point - ActiveMQ Artemis release](./amq-activemq.md)

### AWS Samples

* [Github amazon-mq-workshop](https://github.com/aws-samples/amazon-mq-workshop/tree/master)
* [Github Amazon MQ Network of Brokers](https://github.com/aws-samples/aws-mq-network-of-brokers)

## [Rabbit MQ](https://www.rabbitmq.com/)

The other Open Source project.

## Interesting content to read

* [AWS Active MQ Workshop](https://catalog.us-east-1.prod.workshops.aws/workshops/0b534eb9-fdfb-49f0-8df4-ebccca71a9eb/en-US)
* [Amazon MQ RabbitMQ workshop](https://catalog.us-east-1.prod.workshops.aws/workshops/88db3818-a8bb-4f5c-acf9-e57fa7a129b6/en-US)
* [Create broker AWS CLI command.](https://awscli.amazonaws.com/v2/documentation/api/latest/reference/mq/create-broker.html)
* [Amazon MQ CLI](https://github.com/antonwierenga/amazonmq-cli).
* [Implementing enterprise integration patterns with AWS messaging services: point-to-point channels.](https://aws.amazon.com/blogs/compute/implementing-enterprise-integration-patterns-with-aws-messaging-services-point-to-point-channels/)