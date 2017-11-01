[![Build Status](https://travis-ci.org/ExpediaDotCom/haystack-agent.svg?branch=master)](https://travis-ci.org/ExpediaDotCom/haystack-agent)
[![License](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg)](https://github.com/ExpediaDotCom/haystack/blob/master/LICENSE)

# haystack-agent
This repo contains haystack-agent that can be run as a side-car container or a standalone agent on the same host of micro service application.
One need to add the haystack [client library](https://github.com/ExpediaDotCom/haystack-client-java) in its service application to push the spans to the haystack agent running locally(or side car container).
The haystack agent runs as a grpc server that accepts the [spans](https://github.com/ExpediaDotCom/haystack-idl). 
The agent will collect the spans and dispatch them to one or more sinks like kafka, aws-kinesis etc. depending upon the configuration.
We provide few out of the box dispatchers for instance to kafka and aws-kinesis. Since we follow service provider interface design pattern, it is easy to extend agents as well as dispatchers.

We encourage writing the open source community to contribute dispatchers in this repo. But you are free to use the agent libraries that are published in maven central to write custom agents or dispatchers in your private repo.


## Building

####
Since this repo contains haystack-idl as the submodule, so use the following to clone the repo
* git clone --recursive git@github.com:ExpediaDotCom/haystack-agent.git .

####Prerequisite: 

* Make sure you have Java 1.8
* Make sure you have maven 3.3.9 or higher
* Make sure you have docker 1.13 or higher


Note : For mac users you can download docker for mac to set you up for the last two steps.

####Build

For a full build, including unit tests you can run -
```
mvn clean package
```
####How to run locally?
Edit dev-config.yaml and set the kafka endpoint correctly and then run
```
java -DHAYSTACK_AGENT_CONFIG_FILE_PATH=dev-config.yaml -jar agent/target/haystack-agent-1.0-SNAPSHOT.jar 
```
This will spin up grpc server on port 8080
