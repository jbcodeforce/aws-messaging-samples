# IBM MQ

## Create a docker for Mac silicon

The information is in [this repository](https://github.com/ibm-messaging/mq-container.git), but it is simple, once cloned do: `make build-devserver`. The created docker image on 11/22/2023 is `ibm-mqadvanced-server-dev:9.3.4.0-arm64`.

The docker compose file in [docker-compose for ibm mq]() can start one instance of IBMMQ for development purpose.

## Point to point code based on JMS


## Install docker on EC2

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
```

### IBM MQ with docker

The docker image to use on Linux AMI 2023 is: `icr.io/ibm-messaging/mq:latest`

Docker compose file to use:

```yaml
version: '3.7'
services:
  ibmmq:
    image: icr.io/ibm-messaging/mq:latest
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

## Logging

https://docs.aws.amazon.com/amazon-mq/latest/developer-guide/security-logging-monitoring-cloudwatch.html
https://docs.aws.amazon.com/amazon-mq/latest/developer-guide/amazon-mq-accessing-metrics.html


## Useful source of information

* [IBM MQ Developer Essentials](https://developer.ibm.com/learningpaths/ibm-mq-badge)