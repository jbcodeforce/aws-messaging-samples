mvn package -DskipTests=true
docker build -t j9r/mq-jms-consumer -f src/main/docker/Dockerfile.jvm .