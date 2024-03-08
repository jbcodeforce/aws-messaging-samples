curl -X 'POST' \
    'http://localhost:8081/carrides' \
    -H 'accept: */*' \
    -H 'Content-Type: application/json' \
    -d '{
    "customerID": "C01",
    "pickup": "Location_1",
    "destination": "Location_2",
    "rideDate": "11/30/2023",
    "rideTime": "10:00",
    "numberOfPassengers": 2
    }'