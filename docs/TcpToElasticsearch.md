# TCP to Elasticsearch

Goal was to measure the max write speed to Elasticsearch in DCOS.  

## Overview
- Input
  - Messages are lines (simFile.json). Each line is a JSONObject to be stored in Elasticsearch
  - Lines written to Kafka Topic
- Processing (none)
- Output
  - Take input from Kafka Topic 
  - Write Messages to Elasticsearch

## Assumptions

You've done something line [IntroTest](IntroTest.md)
- You've already installed DCOS and Kafka. 
- You have a Test Server setup

## Install Elasticsearch

From Universe->Packages

Search for Elasticsearch

Advanced Install 
Set Instances: 1


# Input File

The simFile_10000_10s.json file.

Sample of the lines
<pre>
{"rt": "IAH-IAD", "dtg": "19-Jul-2016 08:46:06.006", "lon": -88.368, "brg": 57.53489, "tm": 1468935966122, "lat": 34.02488, "spd": 238.75427650928157, "id": 138}
{"rt": "HER-LTN", "dtg": "19-Jul-2016 08:46:06.006", "lon": 8.50379, "brg": -50.95271, "tm": 1468935966143, "lat": 47.76283, "spd": 294.168437230936, "id": 414}
{"rt": "BGY-BDS", "dtg": "19-Jul-2016 08:46:06.006", "lon": 15.59388, "brg": 131.03384, "tm": 1468935966153, "lat": 42.23651, "spd": 240.7438369021059, "id": 706}
{"rt": "PDX-MSO", "dtg": "19-Jul-2016 08:46:06.006", "lon": -116.40565, "brg": 78.6733, "tm": 1468935966163, "lat": 46.62344, "spd": 226.42531977397485, "id": 848}
</pre>
 
 Basically the same as the csv file.  Just in JSON format.  This will reduce the burden on the service; allowing it to just read the data from kafka and write to elasticsearch. 


# Create tcp-kafka

This service listens on TCP and writes to a Kafka topic. 

<pre>
{
  "id": "/tcp-kafka",
  "cmd": "java -cp $MESOS_SANDBOX/rt-jar-with-dependencies.jar org.jennings.rt.source.tcp.TcpKafka 5565 kafka simFile $PORT",
  "cpus": 1,
  "mem": 2048,
  "disk": 0,
  "instances": 1,
  "constraints": [
    [
      "hostname",
      "UNIQUE"
    ]
  ],
  "healthChecks": [
    {
      "path": "/",
      "protocol": "HTTP",
      "gracePeriodSeconds": 300,
      "intervalSeconds": 60,
      "timeoutSeconds": 20,
      "maxConsecutiveFailures": 3,
      "ignoreHttp1xx": false,
      "portindex": 0
    }
  ],
  "uris": [
    "http://p2/apps/rt-jar-with-dependencies.jar"
  ]
}
</pre>

# Create kafka-elasticsearch

This service listens on a Kafka topic and writes the data to Elasticsearch index. 

<pre>
{
  "id": "/kafka-elasticsearch",
  "cmd": "java -cp rt-jar-with-dependencies.jar org.jennings.rt.sink.kafka.elasticsearch.KafkaElasticsearch kafka simFile group1 elasticsearch - sink simFile 20000 $PORT0",
  "cpus": 2,
  "mem": 2048,
  "disk": 0,
  "instances": 1,
  "healthChecks": [
    {
      "path": "/",
      "protocol": "HTTP",
      "portIndex": 0,
      "gracePeriodSeconds": 300,
      "intervalSeconds": 60,
      "timeoutSeconds": 20,
      "maxConsecutiveFailures": 3,
      "ignoreHttp1xx": false
    }
  ],
  "uris": [
    "http://p2/apps/rt-jar-with-dependencies.jar"
  ]
}
</pre>


# Run Tests

You can feed TCP messages from a file.

On the test Server.

<pre>
java -cp target/Simulator.jar com.esri.simulator.Tcp tcp-kafka.marathon.mesos 5565 simFile_1000_10s.json 100 3000
</pre>

In the stdout of the kafka-elasticsearch you'll see the number of features processed and the rate they were written to Elasticsearch.  

*NOTE:* If you input something that is not valid JSON (delimited lines) to Kafka; you'll get errors in the stderr that indicate that the input was not valid JSON.

|Num Events|Rate Simulator|tcp-kafka|kafka-elasticsearch|
|---|---|---|---|
|3,000,000|30,000|28,200|25,100|
|6,000,000|60,000|54,200|21,900|




