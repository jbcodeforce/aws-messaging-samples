# [Amazon MQ Studies](https://jbcodeforce.github.io/aws-messaging-samples)


### Code samples and local test

In development mode we can use docker to run one artemis broker. Start with `docker compose -f artemis-docker-compose.yaml up -d`, and then the different `quarkus dev` under each producer or consumer folder.

We can validate the console at: [http://localhost:8161/console](http://localhost:8161/).

We can also test within the docker image:

```sh
docker exec artemis /home/jboss/broker/bin/artemis producer --destination demoqueue   --message-size 1024 --message-count 10
# in another terminal
docker exec artemis /home/jboss/broker/bin/artemis consumer --destination demoqueue   --message-count 10 --verbose
```




#### AMQP

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

* Deployment to ECR, and run on Fargate


Read from [book view](https://jbcodeforce.github.io/aws-messaging-samples)

## Building this booklet locally

The content of this repository is written with markdown files, packaged with [MkDocs](https://www.mkdocs.org/) and can be built into a book-readable format by MkDocs build processes.

1. Install MkDocs locally following the [official documentation instructions](https://www.mkdocs.org/#installation).
1. Install Material plugin for mkdocs:  `pip install mkdocs-material` 
2. `git clone https://github.com/jbcodeforce/aws-messaging-samples.git` _(or your forked repository if you plan to edit)_
3. `cd aws-studies`
4. `mkdocs serve`
5. Go to `http://127.0.0.1:8000/` in your browser.

### Building this booklet locally but with docker

In some cases you might not want to alter your Python setup and rather go with a docker image instead. This requires docker is running locally on your computer though.

* docker run --rm -it -p 8000:8000 -v ${PWD}:/docs squidfunk/mkdocs-material
* Go to http://127.0.0.1:8000/ in your browser.

### Pushing the book to GitHub Pages

1. Ensure that all your local changes to the `main` branch have been committed and pushed to the remote repository. `git push origin main`
1. Run `mkdocs gh-deploy --remote-branch main` from the root directory.


## Project Status

* [09/2023] Started this project

## Contributors

* Lead developer [Jerome Boyer](https://www.linkedin.com/in/jeromeboyer/)