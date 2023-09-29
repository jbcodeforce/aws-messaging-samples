# An Orchestrator using request/response with queues

## Requirements

* Expose GET and POST /orders api
* On POST or PUT the orders is sent to another service to act on it via a orders queue, and get the response to orders-reply queue
* Support once and only once semantic
* Expose a POST /orders/simulation to run n order creation with random data, to support failover demonstration.

## Code approach

## Run locally

