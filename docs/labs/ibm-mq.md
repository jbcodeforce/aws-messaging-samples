# IBM MQ Labs

This note regroups explanations of the different code samples to run on top of IBM MQ.

## Create a IBM MQ docker image for Mac silicon

With MAC M silicon, we need a different docker image, the information to build such image is in [this repository](https://github.com/ibm-messaging/mq-container.git), but it is simple, once the repository cloned do: `make build-devserver`. The created docker image on 11/22/2023 is `ibm-mqadvanced-server-dev:9.3.4.0-arm64`.

The docker compose file in [docker-compose for ibm mq](https://github.com/jbcodeforce/aws-messaging-study/blob/main/ibm-mq/src/docker-compose.yaml) can start one instance of IBM MQ broker to be used for development purpose.

## One-way message code based on JMS

For the first demonstration we take a simple JMS producer to IBM MQ queue to a JMS Consumer. It is a point-to-point channel using queue. Nothing fancy, but interesting to see the change to the configuration to work with MQ. Here is the simple diagram

![](./diagrams/p2p-mq-jms.drawio.png)

* For end to end demonstration:

    1. Start docker compose with on IBM MQ broker under src: `docker-compose up -d`
    1. Connect to the Console at [https://localhost:9443](https://localhost:9443), accept the risk on the non-CA certificate, and use admin/passw0rd to access the console.

* While developing the code

  1. Start the special docker compose to have only IBM MQ server container running: `docker-compose -f dev-dc.yaml up -d`

## Install docker on EC2

Install docker, docker compose, start the service, add ec2-user to docker group.
```sh
sudo yum install -y docker
sudo service docker start
sudo usermod -a -G docker ec2-user
sudo chkconfig docker on
sudo curl -L https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m) -o /usr/local/bin/docker-compose
 sudo chmod +x /usr/local/bin/docker-compose
# logout - login back
docker info
docker-compose version
# logout and login again with ec2-user - to avoid the permission denied on docker socket.
docker images 
```

### IBM MQ with docker

* The docker image to use on Linux AMI 2023 is: `icr.io/ibm-messaging/mq:latest`. 

```sh
docker pull icr.io/ibm-messaging/mq:latest
```

* Create a docker compose file to use this image:

```yaml
version: '3.7'
services:
  ibmmq:
    image: icr.io/ibm-messaging/mq:latest
    docker_name: ibmmq
    ports:
        - '1414:1414'
        - '9443:9443'
        - '9157:9157'
    volumes:
        - qm1data:/mnt/mqm
    stdin_open: true
    tty: true
    restart: always
    environment:
        LICENSE: accept
        MQ_QMGR_NAME: QM1
        MQ_ADMIN_PASSWORD: passw0rd
        MQ_APP_PASSWORD: passw0rd
        MQ_ENABLE_METRICS: true
        MQ_DEV: false
volumes:
  qm1data:
```

* Modify the security group of the EC2 instance to add an inbound rule for custom TCP on port 9443, and another one for TCP port 1414.

* Start the MQ server

```sh
docker-compose up -d
```

* Access the IBM Console via the EC2 public URL with port 9443, accept the risk, the user is admin.


## Logging

https://docs.aws.amazon.com/amazon-mq/latest/developer-guide/security-logging-monitoring-cloudwatch.html
https://docs.aws.amazon.com/amazon-mq/latest/developer-guide/amazon-mq-accessing-metrics.html


## Useful source of information

* [IBM MQ Developer Essentials](https://developer.ibm.com/learningpaths/ibm-mq-badge)
* [MQ Developer Cheat sheet:](https://developer.ibm.com/articles/mq-dev-cheat-sheet/) useful for MQRC_NOT_AUTHORIZED error.
* [Configuring connections between the client and server](https://www.ibm.com/docs/en/ibm-mq/9.3?topic=configuring-connections-between-client-server): 