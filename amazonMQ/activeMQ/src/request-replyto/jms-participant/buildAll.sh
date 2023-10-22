mvn package -DskipTests=true
docker build -t jbcodeforce/jms-participant -f src/main/docker/Dockerfile.jvm .