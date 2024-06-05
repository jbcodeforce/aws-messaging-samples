# Active MQ

This section is a quick summary from [ActiveMQ Artemis version product documentation](https://activemq.apache.org/components/artemis/documentation/), ActiveMQ [classic documentation](https://activemq.apache.org/components/classic/documentation) and Amazon MQ [ActiveMQ engine documentation](https://docs.aws.amazon.com/amazon-mq/latest/developer-guide/working-with-activemq.html) for Active MQ 5.17.3 deployment as a managed service.

## [The Open Source project](https://activemq.apache.org/)

Active MQ is an Open Source software, multi-protocol, java based message broker. ActiveMQ has two main version of the product Active MQ 5.x (or classic) and Artemis 2.x which supports Jakarta Messsaging 3.1. It also supports embedding the broker in a java app.

It supports message load balancing, HA. Multiple connected "master" brokers can dynamically respond to consumer demand by moving messages between the nodes in the background.

Amazon MQ - Active MQ engine supports the Classic version.

Active MQ supports different messaging patterns: **queue** and **topic**:

* Queue supports point to point, and request/replyTo pattern.
* Queue can have multiple senders and consumers, the message will be load balanced between consumers. Messages acknowledged are removed from the queue.
* Message with a TTL will be removed from queue, without being consumed.


* Topic supports pub/sub.
* With Topic, receiver starts to receive only the new messages, that are being sent by the sender. Messages sent to topic without consumer are lost.
* Topics support the fan-out pattern. All Messages sent by any senders are received by all connected receivers.


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


### Configurations

A configuration contains all of the settings for the ActiveMQ brokers, in XML format. See the [product documentation](https://activemq.apache.org/version-5-xml-configuration) to get what can be defined.

The configuration parts to consider for any deployment:

* What transport connector to enable for which protocol (amqp, openwire, ...).
* What persistence to use as backend to save messages until read. The default persistence mechanism is the KahaDB store.
* The need to expose JMX
* Control Flow for back preassure management in case of slow consumers. Persistence time.


It is possible to configure users and groups, and then the `authorizationMap` so a specific queue or topic can only be accessed by a specific user/app (The declaration below, allows user1 to manage, write and read from `queue.user1`, but not user2, who is allowed admin, read and write on `topic.user2`): 

```xml
<authorizationPlugin>
    <map>
    <authorizationMap>
        <authorizationEntries>
          <authorizationEntry admin="admin,activemq-webconsole" queue="&gt;" read="admin,activemq-webconsole" write="admin,activemq-webconsole"/>
          <authorizationEntry admin="admin,activemq-webconsole" topic="&gt;" read="admin,activemq-webconsole" write="admin,activemq-webconsole"/>
          <authorizationEntry admin="admin,user1" queue="queue.user1" read="user1" write="user1"/>
          <authorizationEntry admin="admin,user2" read="user2" topic="topic.user2" write="user2"/>
          <authorizationEntry admin="admin,user1,user2" read="admin,user1,user2" topic="ActiveMQ.Advisory.&gt;" write="admin,user1,user2"/>
        </authorizationEntries>
        <tempDestinationAuthorizationEntry>
        <tempDestinationAuthorizationEntry admin="tempDestinationAdmins" read="tempDestinationAdmins" write="tempDestinationAdmins"/>
        </tempDestinationAuthorizationEntry>
    </authorizationMap>
    </map>
</authorizationPlugin>
```

In order to apply the modifications done to the broker configuration, the broker must be rebooted. A reboot can be scheduled, and use specific configuration revision to specify which configuration updates to apply.

### Monitoring

Most of the management and monitoring is done via the Console or the JMX MBeans. See the [monitoring Lab](./labs/activemq-monitoring.md) for JMX local management, and Amazon MQ monitoring.

## Active MQ Topologies

The [Active product documentation HA chapter](https://activemq.apache.org/components/artemis/documentation/) gives all the details on the different topologies supported. Here are the important points to remember:

* Use Live/backup node groups when more than two brokers are used.
* A backup server is owned by only one live server.
* Two strategies for backing up a server **shared store** and **replication**.
* When using a **shared store**, both live and backup servers share the same entire data directory using a **shared file system** (SAN).

    ![](./diagrams/amq-shared-st.drawio.png)

    **Figure 1: Active/standby shared storage**

* Only persistent message data will survive failover.
* With **replication** the data filesystem is not shared, but replicated from live to standby.  At start-up the backup server will first need to synchronize all existing data from the live server, which brings lag. This could be minimized.

    ![](./diagrams/amq-replica.drawio.png)

    **Figure 2: Active/standby replicate storage**

* With replicas when live broker restarts and failbacks, it will replicate data from the backup broker with the most fresh messages.
* Brokers with replication are part of a cluster. So `broker.xml` needs to include cluster connection. Live | backup brokers are in the same node-group.

### Mesh

We can choose a network of brokers with multiple active/standby brokers, like a broker Mesh. This topology is used to increase the number of client applications. Any one of the two Active/Stanby brokers can be active at a time with messages stored in a **shared durable storage**. There is no single point of failure as in client/server or hub and spoke topologies. A client can failover another broker improving high availability. 

The following diagram illustrates a configuration over 3 AZs, and the corresponding [CloudFormation template can be found here](https://s3.amazonaws.com/amazon-mq-workshop/CreateAmazonMQWorkshop.yaml).

![](./diagrams/mq-mesh.drawio.png)

**Figure 3: Active MQ mesh cluster deployment**

Each broker can accept connections from clients. The client endpoints are named `TransportConnectors`. Any client connecting to a broker uses a failover string that defines each broker that the client can connect to send or receive messages.

```sh
amqp+ssl://b-5......87c1e-1.mq.us-west-2.amazonaws.com:5671
amqp+ssl://b-5......87c1e-2.mq.us-west-2.amazonaws.com:5671
```

In order to scale, client connections can be divided across brokers. 

Because those brokers are all connected using network connectors, when a producer sends messages to say NoB1, the messages can be consumed from NoB2 or from NoB3. This is because `conduitSubscriptions` is set to false.

Essentially we send messages to any brokers, and the messages can still be read from a different brokers.

Brokers are connected with each other using `OpenWire` network connectors. Within each broker configuration, for each queue and topic, there are a set of `networkConnector` items defining connection from the current broker and to the two other brokers in the mesh. So each broker has a different networkConnector, to pair to each other broker.

```xml
  <networkConnectors>
    <networkConnector conduitSubscriptions="false" consumerTTL="1" messageTTL="-1" name="QueueConnector_ConnectingBroker_1_To_2" uri="masterslave:(ssl://b-c2....2-1.mq.us-west-2.amazonaws.com:61617,ssl://b-c2...2-2.mq.us-west-2.amazonaws.com:61617)" userName="mqadmin">
      <excludedDestinations>
        <topic physicalName="&gt;"/>
      </excludedDestinations>
    </networkConnector>
    <networkConnector conduitSubscriptions="false" consumerTTL="1" messageTTL="-1" name="QueueConnector_ConnectingBroker_1_To_3" uri="masterslave:(ssl://b-ad...647-1.mq.us-west-2.amazonaws.com:61617,ssl://b-ad...d747-2.mq.us-west-2.amazonaws.com:61617)" userName="mqadmin">
      <excludedDestinations>
        <topic physicalName="&gt;"/>
      </excludedDestinations>
    </networkConnector>
```

The messages do not flow to other brokers if no consumer is available.

The duplex attribute on `networkConnector` essentially establishes a two-way connection on the same port. This would be useful when network connections are traversing a firewall and is common in *Hub and Spoke* broker topology. In a Mesh topology, it is recommended to use explicit unidirectional networkConnector as it allows flexibility to include or exclude destinations.

Because these brokers are all connected using network connectors, when a producer sends messages to say NoB1, the messages can be consumed from NoB2 or from NoB3.


### Hub and Spoke

For Hub and Spoke a central broker dispatches to other connected broker.

![](./diagrams/hub-spoke.drawio.png)

**Figure 4: Hub - spoke topology**

### Connection from client app

Once deployed there are 5 differents end points to support the different protocols:

* OpenWire – ssl://xxxxxxx.xxx.com:61617
* AMQP – amqp+ssl:// xxxxxxx.xxx.com:5671
* STOMP – stomp+ssl:// xxxxxxx.xxx.com:61614
* MQTT – mqtt+ssl:// xxxxxxx.xxx.com:8883
* WSS – wss:// xxxxxxx.xxx.com:61619

As of Dec 2023, Amazon MQ doesn't support Mutual Transport Layer Security (mTLS) authentication.

In active/standby deployment, any one of the brokers can be active at a time. Any client connecting to a broker uses a failover string that defines each broker that the client can connect to.

```sh
failover:(ssl://b-9f..7ac-1.mq.eu-west-2.amazonaws.com:61617,ssl://b-9f...c-2.mq.eu-west-2.amazonaws.com:61617)
```

Adding failover in broker url ensures that whenever server goes up, it will reconnect it immediately. [See Active MQ documentation on failover](https://activemq.apache.org/failover-transport-reference.html)

When the active broker reboots, the client applications may report issue but reconnect to the backup broker. Below is an example of logs:

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


## Storage

The [ActiveMQ message storage](https://activemq.apache.org/amq-message-store) is an embeddable transactional message storage solution. It uses a transaction journal to support recovery. Messages are persisted in data logs (up to 32mb size) with reference to file location saved in [KahaDB](https://activemq.apache.org/kahadb.html), in memory. Messages are in memory and then periodically inserted in the storage in the frequency of `checkpointInterval` ms. Version 5.14.0 introduces journal synch to disk strategy: `always` ensures every journal write is followed by a disk sync (JMS durability requirement). 

Message data logs includes messages/acks and transactional boundaries.
Be sure to have the individual file size greater than the expected largest message size.

Also broker who starts to have memory issue, will throttle the producer or even block it. See [this Producer flow control article](https://activemq.apache.org/producer-flow-control.html) for deeper explanation and configuration per queue.

Messages can be archived into separate logs.

See [the product documentation for persistence configuration.](https://activemq.apache.org/amq-message-store)


## FAQs

???- question "How to create queue or resources?"
    With open source Active MQ, we can use JMS API as they can be created dynamically via code, or use JMX. Static definitions can be done in the broker.xml file:
    
    ```
    ```

???- question "What is the advantage of replicas vs shared storage?"
    Shared storage needs to get SAN replication to ensure DR at the storage level. If not the broker file system is a single point of failure. It adds cost to the solution but it performs better. Replicas is ActiveMQ integrate solution to ensure High availability and sharing data between brokers. Slave broker copies data from Master. States of the brokers are not exchanged with replicas, only messages are. For Classic, JDBC message store could be used. Database replication is then used for DR. When non durable queue or topic are networked, with failure, inflight messages may be lost.

???- question "What is the difference between URL failover and implementing an ExceptionListener?"
    Java Messaging Service  has no specification on failover for JMS provider. When broker fails, there will be a connection Exception. The way to manage this exception is to use the asynchronous `ExceptionListener` interface which will give developer maximum control over when to reconnect, assessing what type of JMS error to better act on the error. ActiveMQ offers the failover transport protocol, is for connection failure, and let the client app to reconnect to another broker as part of the URL declaration. Sending message to the broker will be blocked until the connection is restored. Use `TransportListener` interface to understand what is happening. This is a good way to add logging to the application to report on the connection state.
    

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
    * Amazon MQ does not support Jolokia.

## Code samples

* [Point to point producer to consumer using JMS](./labs/ow-pt-to-pt-jms.md) running locally to start playing with Active MQ classic, or deploy the two apps and the Broker on Amazon MQ with AWS CDK as infrastructure as code.

## To address in the future

* amqp client 
* reactive messaging with brocker as channel
* stomp client
* openwire client
