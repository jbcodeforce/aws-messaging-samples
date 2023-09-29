mvn clean package -DskipTests=true
docker build -t jbcodeforce/jms-producer -f src/main/docker/Dockerfile.jvm . 