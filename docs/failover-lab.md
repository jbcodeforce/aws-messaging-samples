# Local failover demo

The demonstration is based on a shared store deployment as illustrated by the following figure

![](./diagrams/amq-shared-st.drawio.png)

The docker image to use is  [activeMQ/failover-pt-pt/mq-act-stby-docker-compose.yml](https://github.com/jbcodeforce/aws-messaging-samples/tree/main/activeMQ/failover-pt-pt/mq-act-stby-docker-compose.yml). 

The live broker configuration is in config/broker-1.xml and has the following declaration

```xml
<ha-policy>
    <shared-store>
        <master>
            <failover-on-shutdown>true</failover-on-shutdown>
        </master>
    </shared-store>
</ha-policy>
```

While the backup server has 

```xml
<ha-policy>
    <shared-store>
        <slave>
            <allow-failback>true</allow-failback>
        </slave>
    </shared-store>
</ha-policy>
```

To test the failover, start the active/passive brokers, and uses two different broker configurations in `config` folder.

```sh
docker compose -f mq-act-stby-docker-compose.yml up -d
# the container names include active and passive
```
