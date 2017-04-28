# Load Balancing using Marathon-LB 

During testing I noted that using endpoint did not effectively load balance between instances.   

Installed and configured Marathon-LB
- Installed from DCOS Universe
- Advanced Installation
 - Uncheck: BIND-HTTP-HTTPS
 - HAPROXY-GROUP: internal
 - NAME: marathon-lb-internal
 - ROLE: *

Deploy Service with Configurations for ports. The following example app uses two ports. The "default" port $PORT0 is used for service health and counts. The tcp-kafka app listens on the "service" port $PORT1.

<pre>
{
  "id": "/tcp-kafka",
  "cmd": "java -cp $MESOS_SANDBOX/rt-jar-with-dependencies.jar org.jennings.rt.source.tcp.TcpKafka $PORT1 kafka simFile $PORT0",
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
  "labels": {
    "HAPROXY_GROUP": "internal"
  },
  "portDefinitions": [
    {
      "protocol": "tcp",
      "name": "default",
      "labels": {}
    },
    {
      "protocol": "tcp",
      "name": "service",
      "labels": {}
    }

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
      "portIndex": 0
    }
  ],
  "uris": [
    "http://a91/apps/rt-jar-with-dependencies.jar"
  ]
}
</pre>

During deployment Marathon will assign a port in 10000 to 10200 range for Marathon-LB to listen on. You can now run Simulator TCP app to send inputs to Marathon-LB on this port and it will distribute the requests to the instances of tcp-kafka.

<pre>
java -cp Simulator-jar-with-dependencies.jar com.esri.simulator.Tcp marathon-lb-internal.marathon.mesos 10102 simFile_10000_10s.dat 200000 2000000
</pre>

