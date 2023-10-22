mvn package -DskipTests=true
docker build -t jbcodeforce/jms-classic-participant -f src/main/docker/Dockerfile.jvm .