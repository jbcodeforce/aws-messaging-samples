echo "###########################"
echo "# start the solution"

echo "# Start some order generation"
./e2e/startNorders.sh 100 &
sleep 10
echo "# Stop active broker"
docker stop active
echo "docker logs participant"
docker logs participant
