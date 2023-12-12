# Active MQ & Amazon MQ 

This section is a quick summary from [ActiveMQ Artemis version product documentation](https://activemq.apache.org/components/artemis/documentation/), ActiveMQ [classic documentation](https://activemq.apache.org/components/classic/documentation) and Amazon MQ [ActiveMQ engine documentation](https://docs.aws.amazon.com/amazon-mq/latest/developer-guide/working-with-activemq.html) for Active MQ 5.17.3 deployment as a managed service.

## The Open source

ActiveMQ has two main version of the product Active MQ 5.x (or classic) and Artemis 2.x which supports Jakarta Messsaging 3.1.

### Value propositions

* Java 11+, JMS 2.0, Jakarta Messaging 3.0
* [Protocols](https://activemq.apache.org/protocols) supported: STOMP, AMQP, [OpenWire](https://activemq.apache.org/wire-protocol), MQTT, NMS (.Net), CMS (C++),  HornetQ, core Artemis API.
* Support Queues and Topics for pub/sub
* Performance with message persistence.
* Integrated with Java EE application server or embbeded in a java app, or standalone using lightweight netty server.
* HA solution with automatic client failover
* Flexible clustering
* Messages can be ordered by message group
* Message filtering using selectors to perform content based routing
* Unlimited message size so there is not need to plan for unexpected messages
* Message Delay and scheduling
* Distribute transactions to manage complex multi stage transactions such as database access
* Virtual Topics and composite destinations
* Complex redelivery policy


## Topologies

### Active Standby

* Active / Standby: this topology uses a pair of brokers, one getting all the connection and traffic, the other in standby, ready to take the traffic in case of failure on active broker. The persistence is supported by a Storage Area Network.

    ![](./diagrams/amq-efs-2az.drawio.png)

* Amazon [Elastic File System](https://docs.aws.amazon.com/efs/latest/ug/whatisefs.html) is the serverless file system, used here to persist messages from Active MQ. We can mount the EFS file systems on on-premises data center servers when connected to your Amazon VPC with AWS Direct Connect or AWS VPN.

#### Hybrid cloud with AWS

* During migration to the cloud, we need to support hybrid deployment where existing applications on-premises consume messages from queues or topics defined in Amazon MQ - Active MQ engine. The following diagram illustrates different possible integrations and the deployment of active/standby brokers in 2 availability zones.

    ![](./diagrams/on-prem-to-activemq.drawio.png)

    * The on-premises applications or ETL jobs access the active broker, using public internet or private connection with Amazon Direct Connect, or site-to-site VPN.
    * For the public access, the internet gateway routes the traffic to a network load balancer (layer 4 TCP routing), which is also HA (not represented in the figure), to reach the active Broker.
    * The public internet traffic back from the Active MQ queue or topic to the consumer is via a NAT gateway. NAT gateways are defined in both public subnets for HA.
    * When using private gateways, the VPC route tables includes routes to the CIDR of the on-premises subnets.
    * Security group defines firewall like policies to authorize inbound and outbound traffic. The port for Active MQ needs to be open. Below is such declaration:
    * EFS is used as a shared file system for messages persistence. 
    * The standby broker is linked to the active broker and ready to take the lead in case of active broker failure.
    * For higher bandwidth and secured connection, Direct Connect should be used and then the communication will be via private gateway end point.
    * Lambda function may be used to do light processing like data transformation, or data enrichment and then to call directly SaaS services. When more complex flow, like stateful flows, are needed, Amazon Step function can also be used (also serverless).

### Mesh

* Network of brokers with multiple active/standby brokers, like a broker Mesh. This topology is used to increase the number of client applications. There is no single point of failure as in client/server or hub and spoke topologies. A client can failover another broker improving high availability.

    ![](./diagrams/mq-mesh.drawio.png)

* Amazon MQ propose a mesh network of single-instance brokers with non replicated files as they use EBS volume.

    ![](./diagrams/mq-mesh-single.drawio.png)

### Hub and Spoke

For Hub and Spoke a central broker dispatches to other connected broker.

![](./diagrams/hub-spoke.drawio.png)

## Active MQ as Amazon MQ Engine

In the Amazon MQ, the primary AWS resources are the Amazon MQ message broker and its configuration. The broker can be deployed in SINGLE_INSTANCE, ACTIVE_STANDBY_MULTI_AZ, or CLUSTER_MULTI_AZ. The configuration can be done [via the AWS console](./labs/aq-aws-console-lab.md), AWS CLI, Cloud Formation or [CDK](./labs/activemq-cdk.md)

### Security considerations

Active MQ is coming with its own way to define access control to Queue, Topics and Brokers. Amazon MQ uses IAM for creating, updating, and deleting operations on the message broker or configuration, and native ActiveMQ authentication for connection to brokers. The following figure illustrates those security contexts:


![](https://jbcodeforce.github.io/aws-studies/infra/diagrams/messaging/amq-sec-users.drawio.png)

We define three users: 

1. An **IAM administrator** who manages security within an AWS account, specifically IAM users, roles and security policies. This administrator has full access to CloudWatch logs and CloudTrail for API usage auditing. The IAM policy uses action on "mq:" prefix, and possible resources are `broker` and `configuration`. 
1. An **MQ service administrator**, manages the MQ brokers and configuration via the AWS Console, AWS CLI or APIs. This administrator should be able to define brokers and configurations, networking access controls, security groups, and may be anything related to consumer and producer apps. He/she should have access to CloudWatch Logs and CloudTrail logs too. An administrator needs to signin to amazon api and get the [permissions to act on the broker](https://docs.aws.amazon.com/amazon-mq/latest/developer-guide/security-api-authentication-authorization.html#security-permissions-required-to-create-broker) and the underlying EC2 instances.
1. Developer defining queue, topics, and get broker URL and credentials. Developer users can be define in external active directory or in broker configuration file.

Amazon MQ management operations like creating, updating and deleting brokers require IAM credentials and are not integrated with LDAP.

As the Amazon MQ control plane will do operations on other AWS services, there is a service-linked role (`AWSServiceRoleForAmazonMQ`) defined automatically when we define a broker, with the security policies ([AmazonMQServiceRolePolicy](https://console.aws.amazon.com/iam/home#policies/arn:aws:iam::aws:policy/aws-service-role/AmazonMQServiceRolePolicy)) to get the brokers deployed. A service-linked role is a unique type of IAM role that is linked directly to Amazon MQ.


Amazon MQ uses ActiveMQ's Simple Authentication Plugin to restrict reading and writing to destinations. See [this product documentation](https://docs.aws.amazon.com/amazon-mq/latest/developer-guide/security-authentication-authorization.html) for details.

#### LDAP integration

* The LDAP integration is done via [ActiveMQ JAAS plugin](https://activemq.apache.org/security).

    ![](./diagrams/amq-ldap.drawio.png)

* A service account, defined in LDAP, is required to initiate a connection to an LDAP server. It sets up LDAP authentication for the brokers. Client connections are authenticated through this broker-LDAP connection.
* The on-premises LDAP server needs a DNS name, and be opened on port 636.
* When creating broker, we can specify the LDAP login configuration with User Base distinguished name, search filter, role base DN, role base search filter. The user base supplied to the ActiveMQ broker must point to the node in the Directory Information Tree where users are stored in the LDAP server.

As illustrated in figure above, we have to assess the different use cases:

1. A user accessing the MQ console, the authentication to the broker console will go to the LDAP server
1. Applications, producers or consumers, accessing the broker using the transport Connector URL, and authenticate via a service user defined in LDAP.
1. An administrator users access Amazon MQ control plane via API, using AWS CLI, to create brokers, configurations. This user needs to be part of the `amazonmq-console-admins` group.

For user authentication via LDAP we need to define the connection to LDAP in the broker configuration file, which also includes where to search the JMS topic and queue information in the DIT:

```xml
<plugins>
    <jaasAuthenticationPlugin configuration="LdapConfiguration" /> 
    <authorizationPlugin> 
     <map> 
       <cachedLDAPAuthorizationMap
            queueSearchBase="ou=Queue,ou=Destination,ou=ActiveMQ,dc=systems,dc=anycompany,dc=com"
            topicSearchBase="ou=Topic,ou=Destination,ou=ActiveMQ,dc=systems,dc=anycompany,dc=com"
            tempSearchBase="ou=Temp,ou=Destination,ou=ActiveMQ,dc=systems,dc=anycompany,dc=com"
            refreshInterval="300000"
            legacyGroupMapping="false"
        />
         ...
```

* Authorization is done on a per-destination basis (or wildcard, destination set) via the `cachedLdapAuthorizationMap` element, found in the broker’s `activemq.xml`. See [Amazon MQ product doc](https://docs.aws.amazon.com/amazon-mq/latest/developer-guide/security-authentication-authorization.html) for xml examples and [Active MQ doc with OpenLDAP example](https://activemq.apache.org/cached-ldap-authorization-module).
* In LDAP, we can define topics and queues in a Destination OU like: `dn: cn=carrides,ou=Queue,ou=Destination,ou=ActiveMQ,ou=systems,dc=anycompany,dc=com`, within those OU, either a wildcard or specific destination name can be provided (OU=ORDERS.$). Within each OU that represents a destination or a wildcard, we must create three security groups: admin, write, read, to include users or groups who have permission to perform the associated actions.

```ldif
dn: cn=admin,cn=carrides,ou=Queue,ou=Destination,ou=ActiveMQ,ou=systems,dc=anycompany,dc=com 
cn: admin 
description: Admin privilege group, members are roles 
member: cn=admin 
member: cn=webapp 
objectClass: groupOfNames 
objectClass: top 
```

Adding a user to the admin security group for a particular destination will enable the user to create and delete that queue or topic.


#### Connection from client app

Once deployed there are 5 differents end points to support the different protocols:

* OpenWire – ssl://xxxxxxx.xxx.com:61617
* AMQP – amqp+ssl:// xxxxxxx.xxx.com:5671
* STOMP – stomp+ssl:// xxxxxxx.xxx.com:61614
* MQTT – mqtt+ssl:// xxxxxxx.xxx.com:8883
* WSS – wss:// xxxxxxx.xxx.com:61619

Dec 2023, Amazon MQ doesn't support Mutual Transport Layer Security (mTLS) authentication.

In active/standby deployment, any one of the brokers can be active at a time. Any client connecting to a broker uses a failover string that defines each broker that the client can connect to.

```sh
failover:(ssl://b-9f..7ac-1.mq.eu-west-2.amazonaws.com:61617,ssl://b-9f...c-2.mq.eu-west-2.amazonaws.com:61617)
```

Adding failover in broker url ensures that whenever server goes up, it will reconnect it immediately. [See Active MQ documentation on failover](https://activemq.apache.org/failover-transport-reference.html)

??? info "Network mapping"
    On AWS, each of those failover URL are in fact mapped to IP@ of a ENI. Each broker node has two [ENIs connected](https://docs.aws.amazon.com/amazon-mq/latest/developer-guide/connecting-to-amazon-mq.html) to two different networks. The `b-9f...-1` is mapped to 10.42.1.29 for example on subnet 1, while `b-9f...-2` is 10.42.0.92 to subnet 0.

When the active broker reboot, the client applications may report issue but reconnect to the backup broker. Below is an example of logs:

```sh
Transport: ssl://b-d....-2.mq.us-west-2.amazonaws.com/10.42.0.113:61617] WARN org.apache.activemq.transport.failover.FailoverTransport - Transport (ssl://b-d...-2.mq.us-west-2.amazonaws.com:61617) failed , attempting to automatically reconnect: {}
java.io.EOFException
        at java.base/java.io.DataInputStream.readInt(DataInputStream.java:397)
    ...

[ActiveMQ Task-3] INFO org.apache.activemq.transport.failover.FailoverTransport - Successfully reconnected to ssl://b-d...-1.mq.us-west-2.amazonaws.com:61617
```

In the context of cluster mesh, each application may use different failover URL to connect to different brokers.

One sender can have the following URL configuration:

```sh
failover:(ssl://b-650....e-1.mq.us-west-2.amazonaws.com:61617,ssl://b-650...e-2.mq.us-west-2.amazonaws.com:61617)
```

and one consumer with the url:

```sh
failover:(ssl://b-9f69...f-1.mq.us-west-2.amazonaws.com:61617,ssl://b-9f69...f-2.mq.us-west-2.amazonaws.com:61617)
```

The networkConnector in each broker configuration links each broker per pair, and messages flow between brokers using `networkConnectors` only when a consumer demands them. The messages do not flow to other brokers if no consumer is available.

## Server configuration HA and Failover

The [Artemis product documentation HA chapter](https://activemq.apache.org/components/artemis/documentation/) gives all the details on the different topologies supported. Here are the important points to remember:

* Use Live/backup node groups when more than two brokers are used.
* A backup server is owned by only one live server.
* Two strategies for backing up a server **shared store** and **replication**.
* When using a **shared store**, both live and backup servers share the same entire data directory using a **shared file system** (SAN).

    ![](./diagrams/amq-shared-st.drawio.png)

* Only persistent message data will survive failover.
* With **replication** the data filesystem is not shared, but replicated from live to standby.  At start-up the backup server will first need to synchronize all existing data from the live server, which brings lag. This could be minimized.

    ![](./diagrams/amq-replica.drawio.png)

* With replicas when live broker restarts and failbacks, it will replicate data from the backup broker with the most fresh messages.
* Brokers with replication are part of a cluster. So `broker.xml` needs to include cluster connection. Live | backup brokers are in the same node-group.

## Storage

The [ActiveMQ message storage](https://activemq.apache.org/amq-message-store) is an embeddable transactional message storage solution. It uses a transaction journal to support recovery. Messages are persisted in data logs (up to 32mb size) with reference to file location saved in [KahaDB](https://activemq.apache.org/kahadb.html), in memory. Messages are in memory and then periodically inserted in the storage in the frequency of `checkpointInterval` ms. Version 5.14.0 introduces journal synch to disk strategy: `always` ensures every journal write is followed by a disk sync (JMS durability requirement). 

Message data logs includes messages/acks and transactional boundaries.
Be sure to have the individual file size greater than the expected largest message size.

Also broker who starts to have memory issue, will throttle the producer or even block it. See [this Producer flow control article](https://activemq.apache.org/producer-flow-control.html) for deeper explanation and configuration per queue.

Messages can be archived into separate logs.

See [the product documentation for persistence configuration.](https://activemq.apache.org/amq-message-store)


## FAQs

Most of those questions are related to the Open source version, but some to Amazon MQ deploymento of Active MQ.

???- question "What needs to be done to migrate to Artemis"
    As of today Amazon MQ, Active MQ supports on Classic deployment and API. Moving to Artemis, most of the JMS code will work. The project dependencies need to be changed, the ActiveMQ connection factory class is different in term of package names, and if you use Jakarta JMS then package needs to be changed in the JMS producer and consumer classes.

???- question "What is the advantage of replicas vs shared storage?"
    Shared storage needs to get SAN replication to ensure DR at the storage level. If not the broker file system is a single point of failure. It adds cost to the solution but it performs better. Replicas is ActiveMQ integrate solution to ensure High availability and sharing data between brokers. Slave broker copies data from Master. States of the brokers are not exchanged with replicas, only messages are. For Classic, JDBC message store could be used. Database replication is then used for DR. When non durable queue or topic are networked, with failure, inflight messages may be lost.

???- question "What is the difference between URL failover and implementing an ExceptionListener?"
    JMS has no specification on failover for JMS provider. When broker fails, there will be a connection Exception. The way to manage this exception is to use the asynchronous `ExceptionListener` interface which will give developer maximum control over when to reconnect, assessing what type of JMS error to better act on the error. ActiveMQ offers the failover transport protocol, is for connection failure, and let the client app to reconnect to another broker as part of the URL declaration. Sending message to the broker will be blocked until the connection is restored. Use `TransportListener` interface to understand what is happening. This is a good way to add logging to the application to report on the connection state.

???- question "what are the critical metrics / log patterns that should be monitored in respect to MQ logs?"
    CloudWatch metrics has a specific Amazon MQ dashboard with CpuUtilization, CurrentConnectionCount, networking in/ou, producer and consumer counts. We can add out own metrics from a list of broker or queue specific ones. The following may be of interest for storage: (See [this re:post](https://repost.aws/knowledge-center/mq-persistent-store-is-full-errors)):
        
    * Store Percentage Usage
    * Journal Files for Full Recovery: # of journal files that are replayed after a clean shutdown.
    * Journal Files for Fast Recovery: same but for unclean shutdown. (too many pending messages in storage)

    When broker starts to have memory limit for a destination, then producer flow will be throttled, even blocked. (See [this note](https://activemq.apache.org/producer-flow-control.html))
    

???- question "When messages are moved to DLQ?"
    Producer app can set `setTimeToLive` with millisecond parameter. When the message has not being delivered to consumer, ActiveMQ move it to an expiry address, which could be mapped to a dead-letter queue. In fact a TTL set on a producer, will make ActiveMQ creating an `ActiveMQ.DLQ` queue. It is recommended to setup a DLQ per queue or per pair of request/response queues. ActiveMQ will *never* expire messages sent to the DLQ. See [product documentation](https://activemq.apache.org/message-redelivery-and-dlq-handling.html)

    ```xml
    <policyEntry queue="order*">
        <deadLetterStrategy>
            <individualDeadLetterStrategy queuePrefix="DLQ." useQueueForQueueMessages="true"/>
        </deadLetterStrategy>
    </policyEntry>
    ```
    Use the `<deadLetterStrategy> <sharedDeadLetterStrategy processExpired="false" />` to disable DLQ processing.

???- question "What is the constantPendingMessageLimitStrategy parameter?"
    When consumers are slow to process message from topic, and the broker is not persisting message, then messages in the RAM will impact consumer and producer performance. This parameter specifies how many messages to keep and let old messages being replace by new ones. See [slow consumer section]( http://activemq.apache.org/slow-consumer-handling.html) of the product documentation.


???- question "Broker clustering"
    Brokers in a cluster can share the message processing, each broker manages its own storage and connections. A core bridge is automatically created. When message arrives it will be send to one of the broker in a round-robin fashion. It can also distribute to brokers that have active consumers. There are different topologies supported: symmetric cluster where all nodes are connected to each other, or chain cluster where node is connected to two neighbores, . With a symmetric cluster each node knows about all the queues that exist on all the other nodes and what consumers they have.


???- question "Configuring Transport"
    **Acceptor** defines a way in which connections can be made to ActiveMQ broker. Here is one example: 
    ```xml
      <acceptor name="artemis">tcp://172.19.0.2:61616?tcpSendBufferSize=1048576;tcpReceiveBufferSize=1048576;amqpMinLargeMessageSize=102400;protocols=CORE,AMQP,STOMP,HORNETQ,MQTT,OPENWIRE;useEpoll=true;amqpCredits=1000;amqpLowCredits=300;amqpDuplicateDetection=true;supportAdvisory=false;suppressInternalManagementObjects=false</acceptor>
    ```
    **Connectors** define how to connect to the brokers, used when brokers are in cluster or bridged. When a client app, using ClientSessionFactory, uses indirectly connector.

???- question "What are the metrics to assess to decide to move to server mesh topology?"
    Server mesh is used to increase the number of consumers by adding brokers that may replicate messages. Broker's memory usage. Looking at the number of messages a specific consumer has acknowledged (inflight). Number of consumer per queue. Other important metrics are looking at [queue attributes](https://activemq.apache.org/components/artemis/documentation/1.0.0/queue-attributes.html) like size, DLQ content.


???- question "How to be quickly aware of broker is rebooting?"
    Create a CloudWatch alert on the EC2 rebooting event.

???- question "Why using Jolokia with Active MQ?"
    Some key reasons why developers use Jolokia for ActiveMQ:

    * Jolokia allows easy monitoring and management of ActiveMQ brokers and queues/topics via HTTP/JSON. This is more convenient than JMX remoting.
    * It provides remote access to JMX beans without the need to configure JMX ports/SSL etc.
    * Jolokia converts JMX operations to JSON over HTTP. 
    * It allows bulk JMX operations to be performed with a single request. This improves performance compared to remote JMX.
    * It can auto-discover brokers and provide an aggregated view of multiple ActiveMQ instances.
    * There are Jolokia client libraries and tools available for Java, JavaScript, Go etc which simplify working with ActiveMQ via Jolokia.
    * Jolokia is not tied to ActiveMQ specifically and can work across different JMX-enabled applications. This makes it reusable.


## To address

* amqp client 
* reactive messaging with brocker as channel
* stomp client
* openwire client


## Good source of informations

* [My own summary of Amazon MQ](https://jbcodeforce.github.io/aws-studies/infra/messaging/#amazon-mq)