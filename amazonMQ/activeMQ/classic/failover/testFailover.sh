echo "###########################"
echo "# start the solution"

docker compose -f e2e-docker-compose.yml up -d
sleep 40
echo "# Start some order generation"
./e2e/startNorders.sh 100 &
sleep 10
echo "# Stop active broker"
docker stop active
echo "docker logs participant"
docker logs participant
