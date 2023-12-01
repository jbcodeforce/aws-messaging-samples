# [Amazon Messaging Studies](https://jbcodeforce.github.io/aws-messaging-study)

Read the content of this git from the [book view](https://jbcodeforce.github.io/aws-messaging-study)

Also the repository containts code and infrastructure as code content. Most of the code run locally with docker compose. Here is the list of assets:

* Active MQ classic with a simple request-reply JMS based code to demonstrate a classical EDA pattern of exchanging messages between a microservice orchestrator of a business process and a participant to the process.
* Active MQ Artemis, Quarkus app, using Microprofile reactive messaging with point to point, one queue between a producer and consumer.
* Infrastructure as code, common part to create AWS VPC.
* Some simple basic integration pattern for SQS, and SNS
* Same with  MSK.

## Building this booklet locally

The content of this repository is written with markdown files, packaged with [MkDocs](https://www.mkdocs.org/) and can be built into a book-readable format by MkDocs build processes.

1. Install MkDocs locally following the [official documentation instructions](https://www.mkdocs.org/#installation).
1. Install Material plugin for mkdocs:  `pip install mkdocs-material` 
2. `git clone https://github.com/jbcodeforce/aws-messaging-study.git` _(or your forked repository if you plan to edit)_
3. `cd aws-messaging-study`
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