# On-way pattern with Point to Point JMS based producer and consumer

See explanation in the [book view]().

This folder includes two apps, in JMS to produce and consumer CarRide to Active MQ queue.

* jms-producer
* jms-consumer

Build those quarkus app with `./buildAll.sh`

The docker compose file `docker-compose.yaml` is used to run the solution locally.

```
docker compose up -d
```

The Active MQ Console is at  [http://localhost:8161/console]( http://localhost:8161/console)

The IaC folder includes shell scripts to create broker and config on AWS, and a CDK stack to create brokers and deploy the apps on ECS Fargate.  