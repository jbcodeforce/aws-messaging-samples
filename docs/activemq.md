# ActiveMQ study

This section is a quick summary, notes from [Artemis product documentation](https://activemq.apache.org/components/artemis/documentation/) and other research.

## Value propositions

* Java 11+, JMS 2.0, Jakarta Messaging 3.0
* Performance with message persistence.
* Integrated with Java EE application server or embbeded in a java app, or standalone using lightweight netty server.
* HA solution with automatic client failover
* Flexible clustering
* Support communication protocol: AMQP, OpenWire, MQTT, STOMP, HornetQ, core Artemis API.

## Connection from client app

Once dpeloyed there are 5 differents end points to support the different protocols: 

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

In the context of cluster mesh, each application may use different failover URL to connect to different brokers:

![](./diagrams/mq-mesh.drawio.png)

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

The [Artemis product documentation HA chapter](https://activemq.apache.org/components/artemis/documentation/) gives all the details on the different topologies supported. Here are the important points:

* Use Live/backup node groups.
* A backup server is owned by only one live server.
* Two strategies for backing up a server **shared store** and **replication**.
* When using a **shared store**, both live and backup servers share the same entire data directory using a **shared file system** (SAN).

    ![](./diagrams/amq-shared-st.drawio.png)

* Only persistent message data will survive failover.
* With **replication** the data filesystem is not shared, but replicated from live to standby.  At start-up the backup server will first need to synchronize all existing data from the live server, which brings lag. This could be minimized.

    ![](./diagrams/amq-replica.drawio.png)

* With replicas when live broker restarts and failbacks, it will replicate data from the backup broker with the most fresh messages.
* Brokers with replication are part of a cluster. So broker.xml needs to include cluster connection. Live | backup brokers are in the same node-group.

## To address

broker failure - chaos testing
replicas
amqp client 
reactive messaging