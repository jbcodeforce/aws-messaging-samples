mvn clean package -DskipTests=true
docker build -t jbcodeforce/jms-orchestrator  -f src/main/docker/Dockerfile.jvm . 