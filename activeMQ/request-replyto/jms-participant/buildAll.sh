mvn package -DskipTests=true
docker build -t jbcodeforce/jms-consumer -f src/main/docker/Dockerfile.jvm .