# Amazon messaging studies

???- info "Documentation Updates"
    Creation date: September 2023 - Updated 11/30/2023

    Add IBM MQ one-way point to point JMS implementation.

This repository includes a set of demo applications and IaC to play with Amazon MQ, Amazon SQS, SNS. The goal is to complement the existing Amazon workshops by addressing some specifics customers' requests that need more than one hour to respond. This content can be shared and as an open source can be enhanced to support new specific requests. The approach is to give developers or lead architects deeper content to study and reuse.

## Audience

The main readers of this website are people interested in AWS messaging and open-source messaging systems.

* Solution architects
* Lead developer for new distributed system solutions, with interest in AWS and messaging systems

This site is a companion of [AWS Studies site](https://jbcodeforce.github.io/aws-studies/) where are keep summary of AWS products from my personal studies and my [EDA book](https://jbcodeforce.github.io/eda-studies/).

## Amazon MQ

The Amazon MQ workshops contain a lot of very good information to get started with Amazon MQ. This repository addresses some of the specific subjects not covered in detail in those workshops: JMS programming model, clear separation between producer and consumer applications, different ActiveMQ release support, starting developing locally using docker, failover testing. Addressing unit testing, integration test, reactive messaging and reactive programming with queueing systems.

The code will address the standard enterprise integration patterns of one-way point-to-point and request/responce point-to-point. But it may add more in the future.

For the IaC, we use AWS CDK, with different stacks to be able to reuse common infrastructure like VPC, IAM roles, and an optional Cloud9 environment, and finally the broker configuration.

### [Active MQ](https://activemq.apache.org/)

Active MQ is an Open Source software, multi-protocol, java based message broker. The Artemis version is supporting JMS 2.0.  It supports message load balancing, HA. Multiple connected "master" brokers can dynamically respond to consumer demand by moving messages between the nodes in the background.
Amazon MQ / Active MQ engine supports the Classic version.

### Solutions in this repository

* [Request-ReplyTo order ochestrator and participant based on JMS - ActiveMQ Classic release](./labs/classic-req-reply-jms.md)
* [One Way Point-to-Point JMS based producer and consumer - ActiveMQ](./labs/ow-pt-to-pt-jms.md)
* [Request-Response for an order ochestrator and an order process participant based on JMS - ActiveMQ Artemis release](./req-reply-jms.md)
* [AMQP Quarkus app point to point - ActiveMQ Artemis release](./amqp-activemq.md)
* [Infrastructure as Code - VPC stack](./labs/activemq-cdk.md/#common-stack)
* [Infrastructure as Code - ActiveMQ active/standby topology stack](./labs/activemq-cdk.md/#active_passive)
* [IBM MQ JMS one-way point to point solution](./labs/ibm-mq.md)

### AWS Samples

AWS Sample git account includes samples for Amazon MQ, which can be used for inspiration:

* [Github amazon-mq-workshop](https://github.com/aws-samples/amazon-mq-workshop/tree/master)
* [Github Amazon MQ Network of Brokers samples](https://github.com/aws-samples/aws-mq-network-of-brokers)

## [Rabbit MQ](https://www.rabbitmq.com/)

The other Open Source engine using in Amazon MQ.

To Be done.

???- question "How to connect to Rabbit MQ from different vpc or from on-premises?"
    This [Creating static custom domain endpoints with Amazon MQ for RabbitMQ](https://aws.amazon.com/blogs/compute/creating-static-custom-domain-endpoints-with-amazon-mq-for-rabbitmq/) blog presents SSL and DNS resolution to access an NLB to facade brokers. Also the [NLB can be used cross VPCs](https://repost.aws/questions/QUlIpLMYz7Q7W86iJlZJywZw/questions/QUlIpLMYz7Q7W86iJlZJywZw/configure-network-load-balancer-across-vpcs?) that are peered. Need NLB for broker specific TCP based protocol. Security group in the broker specify inbound traffic from the NLB only. NLB target group uses the broker static VPC endpoint address. NLB can also restrict who can acccess it.

## Interesting content to read

* [re:Invent 2018 - Choosing the right messaging services.](https://www.youtube.com/watch?v=4-JmX6MIDDI)
* [AWS Active MQ Workshop](https://catalog.us-east-1.prod.workshops.aws/workshops/0b534eb9-fdfb-49f0-8df4-ebccca71a9eb/en-US)
* [Amazon MQ RabbitMQ workshop](https://catalog.us-east-1.prod.workshops.aws/workshops/88db3818-a8bb-4f5c-acf9-e57fa7a129b6/en-US)
* [Create broker AWS CLI command.](https://awscli.amazonaws.com/v2/documentation/api/latest/reference/mq/create-broker.html)
* [Amazon MQ CLI](https://github.com/antonwierenga/amazonmq-cli).
* [Implementing enterprise integration patterns with AWS messaging services: point-to-point channels.](https://aws.amazon.com/blogs/compute/implementing-enterprise-integration-patterns-with-aws-messaging-services-point-to-point-channels/)