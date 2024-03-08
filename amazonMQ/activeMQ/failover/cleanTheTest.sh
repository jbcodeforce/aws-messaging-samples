echo "###########################"
echo "# Stop the solution"

docker compose -f e2e-docker-compose.yml down
sleep 40
rm -r data
