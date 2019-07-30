[![Build Status](https://travis-ci.org/ExpediaDotCom/haystack-agent.svg?branch=master)](https://travis-ci.org/ExpediaDotCom/haystack-agent)
[![License](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg)](https://github.com/ExpediaDotCom/haystack/blob/master/LICENSE)

# haystack-agent
This repo contains haystack-agent, which can be run as a side-car container or a standalone agent on the host on which
your micro service is running. One needs to add the haystack 
[client library](https://github.com/ExpediaDotCom/haystack-client-java) in the application to push the spans to the
agent.

The span listener of haystack-agent runs as a GRPC server that accepts 
[spans](https://github.com/ExpediaDotCom/haystack-idl/blob/master/proto/span.proto). It collects the spans and 
dispatches them to one or more sinks, depending upon the configuration. Typical sinks are Kafka and AWS Kinesis;
dispatchers for these two sinks are provided "out of the box" in this repository. We strongly encourage the open source 
community to contribute additional dispatchers to this repo, but developers are free to write custom dispatchers
in a private repository.

# Architecture
The haystack-agent uses the [SPI](https://docs.oracle.com/javase/tutorial/ext/basics/spi.html) design architecture.
The fat jar that gets built from this code contains single agent providers with three dispatchers (kinesis, Kafka and HTTP),
as mentioned above and discussed in more detail below.  
The agents are loaded depending upon the configuration that can be provided via a http endpoint or a local file like

```
java -jar bundlers/haystack-agent/target/haystack-agent-<version>.jar --config-provider file --file-path docker/default.conf
```

The main method in AgentLoader class loads and initializes the agents using ServiceLoader.load(). 
Each agent further loads the configured dispatchers using the same ServiceLoader.load() mechanism and everything is 
controlled through configuration.

### Haystack Agent Configuration
The configuration readers are also implemented using the SPI design model. For now, we are only using file config 
provider that is implemented [here](https://github.com/ExpediaDotCom/haystack-agent/tree/master/config-providers/file).
Below is an example configuration that loads a single agent provider that reads protobuf spans over GRPC.
This span agent spins up a GRPC server listening on port 34000 and publishes, via the configured dispatchers. The
sample configuration below configures both a Kinesis and a Kafka dispatcher. The app or microservice needs to use a GRPC 
client to send messages to this haystack-agent.

```
agents {
  spans {
    enabled = true
    port = 34000

    dispatchers {
      kinesis {
        Region = us-west-2
        StreamName = spans
        OutstandingRecordsLimit = 10000
        MetricsLevel = none
      }
      
      kafka {
        bootstrap.servers = kafka-svc:9092
        producer.topic = spans
      }
      
      http {
        url = http://collector-svc:8080/spans
        client.timeout.millis = 500
        client.connectionpool.idle.max = 5
        client.connectionpool.keepalive.minutes = 5
      }
    }
  }
}
```

## How to run agent as docker?
Build the docker image of haystack-agent with 
```
cp bundlers/haystack-agent/target/haystack-agent-*SNAPSHOT.jar bundlers/haystack-agent/target/haystack-agent.jar
docker build -t haystack-agent:latest -f docker/Dockerfile .

```

and run it as a docker container with
```
docker run -e HAYSTACK_PROP_AGENTS_SPANS_DISPATCHERS_KAFKA_BOOTSTRAP_SERVERS=localhost:9092 haystack-agent:latest
```

We bundle default [configuration](./docker/default.conf) with haystack-agent's docker image. However, you can override or add any property using environment variables by adding a prefix 'HAYSTACK_PROP_'. For e.g. if you want to change the kafka producer topic then use 
```
HAYSTACK_PROP_AGENTS_SPANS_DISPATCHERS_KAFKA_PRODUCER_TOPIC=sometopic
```

## Agent Providers
We have one agent provider today that is loaded depending upon the configuration as above.

### Span Proto Agent
This agent listens as a GRPC server on a configurable port and accepts the protobuf span from the clients. The span 
agent is already implemented in the open source repo and it supports kinesis, Kafka and HTTP dispatchers. Please note 
that we bundle only this span proto agent and the AWS Kinesis dispatcher in our fat jar. 

## Dispatchers

### Kinesis Dispatcher
Kinesis dispatcher uses [KPL](https://github.com/awslabs/amazon-kinesis-producer) and we require the following 
configuration properties for it to work properly: 

1. Region - AWS region for e.g. us-west-2
2. StreamName - name of kinesis stream where spans will be published
3. OutstandingRecordsLimit - maximum pending records that are still not published to kinesis. If agent receives more 
dispatch requests, then it sends back 'RATE_LIMIT_ERROR' in the GRPC response.
4. AWS keys - Optional, use them if you want to connect using static AWS access and secret keys
  * AwsAccessKey
  * AwsSecretKey 
5. StsRoleArn - Optional, use it if you want to provide credentials by assuming a role

You can also provide AWS_ACCESS_KEY and AWS_SECRET_KEY as java system property values, or environment variable, 
or use the IAM role for connecting to Kinesis with a DefaultCredentialProvider.

The Kinesis dispatcher can be configured with other 
[KPL properties](https://github.com/awslabs/amazon-kinesis-producer/blob/master/java/amazon-kinesis-producer-sample/default_config.properties)
 in the same way as we do with 'Region'

### Kafka Dispatcher
The Kafka dispatcher uses high level Kafka producer to write the spans to Kafka topic. 
The dispatcher expects a partition key, and the span-agent uses the 
[TraceId](https://github.com/ExpediaDotCom/haystack-idl/blob/master/proto/span.proto) in the span proto object as the
partition key.

```
a. producer.topic - Kafka topic
b. bootstrap.servers - set of bootstrap servers

```
The Kafka dispatcher can be configured with other Kafka producer properties in the same way as bootstrap.servers.

### HTTP Dispatcher
The HTTP dispatcher uses an http client to post spans to a remote collector. 

```
a. url - url for the http span collector (eg: http://collector-svc:8080/spans)
b. client.timeout.millis - timeout in milliseconds for reporting spans to the collector. Defaults to 500 ms.
c. client.connectionpool.idle.max - number of idle connections to keep in the connection pool. Defaults to 5
d. client.connectionpool.keepalive.minutes - keep alive duration in minutes for connections in the connection pool. Defaults to 5.

```

## How to build code?

#### Clone
Since this repo contains haystack-idl as the submodule, so use the following to clone the repo
* git clone --recursive git@github.com:ExpediaDotCom/haystack-agent.git .

#### Prerequisites: 

* Make sure you have Java 1.8
* Make sure you have maven 3.3.9 or higher
* Make sure you have docker 1.13 or higher

Note : For Mac users you can download docker for Mac to set you up for the last two steps.

#### Build

For a full build, including unit tests you can run -
```
mvn clean package
```
#### How to run locally?
Edit dev.conf and set the Kafka endpoint correctly and then run
```
java -jar bundlers/haystack-agent/target/haystack-agent-<version>.jar --config-provider file --file-path docker/dev.conf
```
This will spin up GRPC server on port 8080

### Releases
1. Decide what kind of version bump is necessary, based on [Semantic Versioning](http://semver.org/) conventions.
In the items below, the version number you select will be referred to as `x.y.z`.
2. Update **all** pom.xml files in this project, changing the version element to `<version>x.y.z-SNAPSHOT</version>`. 
Note the `-SNAPSHOT` suffix.
3. Make your code changes, including unit tests.
4. Update the
[ReleaseNotes.md]((https://github.com/ExpediaDotCom/haystack-agent/blob/master/ReleaseNotes.md))
file with details of your changes.
5. Create a pull request with your changes.
6. Ask for a review of the pull request; when it is approved, the Travis CI build will upload the resulting jar file
to the [SonaType Staging Repository](https://oss.sonatype.org/#stagingRepositories).
7. Tag the build with the version number: from a command line, executed in the root directory of the project:
```
git tag x.y.z
git push --tags
```
This will cause the jar file to be released to the 
[SonaType Release Repository](https://oss.sonatype.org/#nexus-search;quick~haystack-agent).
