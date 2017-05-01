# Kafka to Kafka Transformation

Demonstration of Kafka to Kafka Transform including Parsing and GeoTagging/Filtering.

## Overview
- Inputs
  - CSV Messages (simFile.dat) recevied via TCP
  - Set of Polygons loaded from configuration file
- Processing 
  - Parse csv to json; create geometry from lon,lat fields
  - Geotag against 1,000 polygons 
- Output
  - Print to Stdout a sampling of lines that are Geotagged
  
## Sample Lines of Input

Data is simulated aircraft routes. 

<pre>
1468935966122,138,19-Jul-2016 08:46:06.006,IAH-IAD,-88.368,34.02488,238.75427650928157,57.53489
1468935966143,414,19-Jul-2016 08:46:06.006,HER-LTN,8.50379,47.76283,294.168437230936,-50.95271
1468935966153,706,19-Jul-2016 08:46:06.006,BGY-BDS,15.59388,42.23651,240.7438369021059,131.03384
</pre>

Fields:
timestamp,id,dtg,rt,lon,lat,speed,bearing

Types:
long,int,string,string,double,double,double,double

## Assumptions 

As in the [Introduction Test](IntroTest.md) you have
- DCOS is installed and configured
- Test Server is configured 

## Create tcp-kafka service

Create a service for TCP input.
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


## Position the Polygons and Config File for Transform

Copy the File with the Polygons to be checked against.

As root on Test Server

<pre>
mkdir /var/www/html/data
cp /home/azureuser/rt/airports1000FS.json /var/www/html/data/
</pre>

The airports1000FS.json was is the JSON returned from a airports Feature Service. It contains 1,000 polygons.  

Copy the config file.

<pre>
cp /home/azureuser/rt/org.jennings.rt.sink.kafka.kafka.TransformGeotagSimFile.properties /var/www/html/apps/
cd /var/www/html/apps/
cat cp /home/azureuser/rt/org.jennings.rt.sink.kafka.kafka.TransformGeotagSimFile.properties /var/www/html/apps/
</pre>

Edit properties file.

The file contains some configuable parameters used by the TransformGeotagSimFile class which is executed by the KafkaTransformKafka class.  You'll need to set the path for the fenceUrl for your configuration.  The fieldName is the field that contains values that will be added as the geotag field. If filter it true only events that intersect the polygons will passed.

<pre>
fenceUrl=http://p2/data/airports1000FS.json
fieldName=iata_faa
filter=false
</pre>


## Create kafka-transform-kafka service

You may need to aler the uris but here is the example of the JSON for the service.

<pre>
{
  "id": "/kafka-transform-kafka",
  "cmd": "java -cp $MESOS_SANDBOX/rt-jar-with-dependencies.jar org.jennings.rt.sink.kafka.kafka.KafkaTransformKafka kafka simFile group1 org.jennings.rt.sink.kafka.kafka.TransformGeotagSimFile simFileTrans $PORT0",
  "cpus": 2,
  "mem": 4096,
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
      "portIndex": 0,
      "gracePeriodSeconds": 300,
      "intervalSeconds": 60,
      "timeoutSeconds": 20,
      "maxConsecutiveFailures": 3,
      "ignoreHttp1xx": false
    }
  ],
  "uris": [    
    "http://p2/apps/rt-jar-with-dependencies.jar",
    "http://p2/apps/org.jennings.rt.sink.kafka.kafka.TransformGeotagSimFile.properties"
  ]
}
</pre>


## Create kafka-noop-stdout Service

This will pick items off the Transformed Kafka Topic and print them to Stdout.

<pre>
{
  "id": "/kafka-noop-stdout",
  "cmd": "java -cp $MESOS_SANDBOX/rt-jar-with-dependencies.jar org.jennings.rt.sink.kafka.kafka.KafkaTransformStdout kafka simFileTrans group1 noOp 1 $PORT0",
  "cpus": 1,
  "mem": 1024,
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

## Open Logs kafka-noop-stdout

From DCOS navigate to Services -> kafka-noop-stdout -> Logs -> OUTPUT (STDOUT)

## Run a Simulator

From test server run Simulator to send lines from file to TCP input.

<pre>
java -cp target/Simulator.jar com.esri.simulator.Tcp tcp-kafka.marathon.mesos 5565 simFile_1000_10s.dat 200 2000
</pre>

In the output you should see all the events scrolling by a few will have values with geotag info. 

Change the Properties file

vi /var/www/html/apps/org.jennings.rt.sink.kafka.kafka.TransformGeotagSimFile.properties

Change filter=false to filter=true.

In DCOS restart the kafka-transform-kafka.  This will reload the properties file.

Now when you run the Simulation only the geotag'ed events are displayed in the output.  

## Testing Results
The kafka-transform-kafka outputs the throughput rates after each batch of processing.

Running kafka-transform-kafka with 2 cores and 4G RAM single partition.

During test on Azure (DS4v2) and AWS (M4.2xlarge) the max througput for a single process with single kafka partition was about 10,000/s.  During testing on AWS I scalled the number of kafka-transform-kafka instances and was able to increase throughput.

In each case increased the number of kafka partitions to match the number of instances.

|Number of Instances|Max Throughput /s|
|---|---|
| 1 | 10,000 |
| 15 | 135,000 | 
| 15 (unique constraint) | 150,000 |
| 30 (2 instances/server) | 287,000 |
| 45 (3 instsances/server) | 420,000 | 

Each instance used 2 cpu and 4g memory.  For example the 45 instances test used 15 server, 90 cpus, and 180g memory

This test demonstrated that using DCOS I could scale an application to achieve almost linear gain in performance.  






