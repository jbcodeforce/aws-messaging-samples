mvn clean package -DskipTests=true
docker build -t jbcodeforce/jms-classic-orchestrator  -f src/main/docker/Dockerfile.jvm . 