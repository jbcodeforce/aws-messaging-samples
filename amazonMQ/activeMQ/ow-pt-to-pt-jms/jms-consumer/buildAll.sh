mvn package -DskipTests=true
docker build -t j9r/amq-jms-consumer -f src/main/docker/Dockerfile.jvm .