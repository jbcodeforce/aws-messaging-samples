# AMQP

The activeMQ folder includes AMQP clients based on the Quarkus guides for AMQP. In pure dev mode, quarkus starts AMQP broker automatically.

```sh
# in one terminal
mvn -f amqp-quickstart-producer quarkus:dev
# in a second terminal
mvn -f amqp-quickstart-processor quarkus:dev
```

Open http://localhost:8080/quotes.html in your browser and request some quotes by clicking the button.

With docker compose it uses ActiveMQ image.

```sh
mvn -f amqp-quickstart-producer clean package
mvn -f amqp-quickstart-processor clean package
```
