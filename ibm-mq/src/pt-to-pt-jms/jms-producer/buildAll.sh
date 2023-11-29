mvn clean package -DskipTests=true
docker build -t j9r/mq-jms-producer -f src/main/docker/Dockerfile.jvm . 