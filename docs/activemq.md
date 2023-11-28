# ActiveMQ open source study

This section is a quick summary from [ActiveMQ Artemis version product documentation](https://activemq.apache.org/components/artemis/documentation/), ActiveMQ [classic documentation](https://activemq.apache.org/components/classic/documentation) and Amazon MQ [ActiveMQ engine documentation](https://docs.aws.amazon.com/amazon-mq/latest/developer-guide/working-with-activemq.html) for Active MQ 5.17.3 deployment as a managed service.

ActiveMQ has two main version of the product Active MQ 5.x (or classic) and Artemis 2.x which supports Jakarta Messsaging 3.1.

## Value propositions

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

* Active / Standby: a pair of broker, one getting all the connection and traffic, the other in standby, ready to take the traffic in case of failure on active broker.

![](./diagrams/amq-efs-2az.drawio.png)

* Network of brokers with multiple active/standby brokers, like a broker Mesh. This topology is used to increase the number of client applications. There is no single point of failure as in client/server or hub and spoke topologies. A client can failover another broker improving high availability.

![](./diagrams/mq-mesh.drawio.png)

* Amazon MQ propose a mesh network of single-instance brokers with non replicated files as they use EBS volume.

![](./diagrams/mq-mesh-single.drawio.png)

* Hub and Spoke where a central broker dispatches to other broker.

## Connection from client app

Once deployed there are 5 differents end points to support the different protocols:

* OpenWire – ssl://xxxxxxx.xxx.com:61617
* AMQP – amqp+ssl:// xxxxxxx.xxx.com:5671
* STOMP – stomp+ssl:// xxxxxxx.xxx.com:61614
* MQTT – mqtt+ssl:// xxxxxxx.xxx.com:8883
* WSS – wss:// xxxxxxx.xxx.com:61619

Amazon MQ doesn't support Mutual Transport Layer Security (TLS) authentication currently.

In active/standby deployment, any one of the brokers can be active at a time. Any client connecting to a broker uses a failover string that defines each broker that the client can connect to.

```sh
failover:(ssl://b-9f..7ac-1.mq.eu-west-2.amazonaws.com:61617,ssl://b-9f...c-2.mq.eu-west-2.amazonaws.com:61617)
```

Adding failover in broker url ensures that whenever server goes up, it will reconnect it immediately. [See product documentation on failover](https://activemq.apache.org/failover-transport-reference.html)

??? info "Network mapping"
    On AWS, each of those failover URL are in fact mapped to IP@ of a ENI. Each broker node has two [ENIs connected](https://docs.aws.amazon.com/amazon-mq/latest/developer-guide/connecting-to-amazon-mq.html) to two different network. The `b-9f...-1` is mapped to 10.42.1.29 for example on subnet 1, while `b-9f...-2` is 10.42.0.92 to subnet 0.

When the active broker reboot, the client applications may report issue but reconnect to the backup broker:

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

and consumer with url:

```
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
    Producer app can set setTimeToLive with millisecond parameter. When the message has not being delivered to consumer, ActiveMQ move it to an expiry address, which could be mapped to a dead-letter queue. In fact a TTL set on a producer, will make ActiveMQ creating an `ActiveMQ.DLQ` queue. It is recommended to setup a DLQ per queue or may be per pair of request/response queues. ActiveMQ will *never* expire messages sent to the DLQ. See [product documentation](https://activemq.apache.org/message-redelivery-and-dlq-handling.html)

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

???- question "How to connect to MQ from different vpc or from on-premises?"
    This [Creating static custom domain endpoints with Amazon MQ for RabbitMQ](https://aws.amazon.com/blogs/compute/creating-static-custom-domain-endpoints-with-amazon-mq-for-rabbitmq/) blog presents SSL and DNS resolution to access an NLB to facade brokers. Also the [NLB can be used cross VPCs](https://repost.aws/questions/QUlIpLMYz7Q7W86iJlZJywZw/questions/QUlIpLMYz7Q7W86iJlZJywZw/configure-network-load-balancer-across-vpcs?) that are peered. Need NLB for broker specific TCP based protocol. Security group in the broker specify inbound traffic from the NLB only. NLB target group uses the broker static VPC endpoint address. NLB can also restrict who can acccess it.

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


## To address

* amqp client 
* reactive messaging with brocker as channel
* stomp client
* openwire client