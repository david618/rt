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

## Configure Source

Create a service for TCP input.
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


