# Amazon messaging studies

???- info "Documentation Updates"
    Author: Jerome Boyer 

    Creation date: September 2023 - Updated 1/19/2024

    * 11/23: Add IBM MQ one-way point to point JMS implementation.
    * 12/23: Address an asynchronous implementation for S3 event processing
    * 1/24: 

This repository includes a set of demonstrations and studies about messaging services in AWS or as Open Source. The services in scope are: Amazon MQ, Amazon SQS, SNS, MSK and Kinesis. 

The goal is to group personal notes and knowledge gathering around the different technologies, keep references to valuable sources of information, and do some hands-on work to be able to go deeper on some concepts. This content is as-is, does not represent my employer point of view, and can be shared as an open source. It may be useful for developers or solution architects.

## Audience

The main readers of this website are people interested in AWS messaging and open-source messaging systems.

* Solution architects
* Developers, new to distributed system solutions, with interest in AWS and messaging systems

This site is a companion of my [AWS Studies site](https://jbcodeforce.github.io/yarfba) where I keep summaries of AWS products and my [Event-Driven Architecture book](https://jbcodeforce.github.io/eda-studies/).

## Link back to Event-Driven Architecture

The EDA reference architecture, as introduced in the following figure, uses the concept of event backbone, which is a messaging component supporting asynchronous communication between components

![](https://jbcodeforce.github.io/eda-studies/diagrams/eda-hl.drawio.png)

When zooming into this component, we can see queueing systems are very important to support event or message driven integration:

![](https://jbcodeforce.github.io/eda-studies/diagrams/event-backbone.drawio.png)

This repository tries to go deeper in each of those components.

## [Amazon MQ](https://docs.aws.amazon.com/amazon-mq/latest/developer-guide/welcome.html)

Amazon MQ is a managed message broker for RabbitMQ or ActiveMQ. It runs on EC2 servers, and supports multi-AZs deployment with failover.

The Amazon MQ workshops contain a lot of very good information to get started with Amazon MQ. This repository addresses some of the specific subjects not covered in detail in those workshops: JMS programming model, clear separation between producer and consumer applications, deeper dive to the open source versions, starting developing locally using docker, failover testing... Addressing unit testing, integration test, reactive messaging and reactive programming with queueing systems.

The code will address the standard enterprise integration patterns of one-way point-to-point and request/responce point-to-point. But it may add more in the future.

For the IaC, we will use AWS CDK as much as possible, with different stacks to be able to reuse common infrastructure like VPC, IAM roles, and an optional Cloud9 environment, and finally the broker configuration.

[Read more on Active MQ >>>](./activemq.md)


### AWS Samples

AWS Sample git account includes samples for Amazon MQ, which can be used for inspiration:

* [Github amazon-mq-workshop](https://github.com/aws-samples/amazon-mq-workshop/tree/master)
* [Github Amazon MQ Network of Brokers samples](https://github.com/aws-samples/aws-mq-network-of-brokers)

## [Rabbit MQ](https://www.rabbitmq.com/)

The other Open Source engine using in Amazon MQ.

To Be done.

???- question "How to connect to Rabbit MQ from different vpc or from on-premises?"
    This [Creating static custom domain endpoints with Amazon MQ for RabbitMQ](https://aws.amazon.com/blogs/compute/creating-static-custom-domain-endpoints-with-amazon-mq-for-rabbitmq/) blog presents SSL and DNS resolution to access an NLB to facade brokers. Also the [NLB can be used cross VPCs](https://repost.aws/questions/QUlIpLMYz7Q7W86iJlZJywZw/questions/QUlIpLMYz7Q7W86iJlZJywZw/configure-network-load-balancer-across-vpcs?) that are peered. Need NLB for broker specific TCP based protocol. Security group in the broker specify inbound traffic from the NLB only. NLB target group uses the broker static VPC endpoint address. NLB can also restrict who can acccess it.



## Solutions in this repository

Some concrete code samples are part of this repository to demonstrate some of the important concepts or technology:

* [Request-ReplyTo order ochestrator and participant based on JMS - ActiveMQ Classic release](./labs/classic-req-reply-jms.md)
* [One Way Point-to-Point JMS based producer and consumer - ActiveMQ](./labs/ow-pt-to-pt-jms.md)
* [Request-Response for an order ochestrator and an order process participant based on JMS - ActiveMQ Artemis release](./req-reply-jms.md)
* [AMQP Quarkus app point to point - ActiveMQ Artemis release](./amqp-activemq.md)
* [Infrastructure as Code - VPC stack](./labs/activemq-cdk.md/#common-stack)
* [Infrastructure as Code - ActiveMQ active/standby topology stack](./labs/activemq-cdk.md/#active_passive)
* [Process S3 events for multi-tenant bucket with EDA](./labs/sqs/s3-tenants-async-processing.md)
* [IBM MQ JMS one-way point to point solution](./labs/ibm-mq.md)

## Messaging related sources of information

* [re:Invent 2018 - Choosing the right messaging services.](https://www.youtube.com/watch?v=4-JmX6MIDDI)
* [Amazon MQ RabbitMQ workshop](https://catalog.us-east-1.prod.workshops.aws/workshops/88db3818-a8bb-4f5c-acf9-e57fa7a129b6/en-US)
* [Implementing enterprise integration patterns with AWS messaging services: point-to-point channels.](https://aws.amazon.com/blogs/compute/implementing-enterprise-integration-patterns-with-aws-messaging-services-point-to-point-channels/)