#!/bin/bash

namebase="tk"
topicbase="sf"

broker="a81:9092"
taskcfgfile="tktemp.json"

for i in $(seq 1 $1); do 

name=${namebase}${i}
topic=${topicbase}${i}

cat > ${taskcfgfile} << ZZZ
{
  "id": "/${name}",
  "cmd": "java -cp \$MESOS_SANDBOX/rt-jar-with-dependencies.jar org.jennings.rt.source.tcp.TcpKafka \$PORT0 ${broker} ${topic} \$PORT1",
  "container": {
    "type": "MESOS"
  },
  "cpus": 0.5,
  "disk": 0,
  "fetch": [
    {
      "uri": "http://p1:81/apps/rt-jar-with-dependencies.jar",
      "extract": false,
      "executable": false,
      "cache": false
    }
  ],
  "healthChecks": [
    {
      "gracePeriodSeconds": 300,
      "intervalSeconds": 60,
      "maxConsecutiveFailures": 3,
      "portIndex": 1,
      "timeoutSeconds": 20,
      "delaySeconds": 15,
      "protocol": "MESOS_HTTP",
      "path": "/"
    }
  ],
  "instances": 1,
  "mem": 1024,
  "gpus": 0,
  "networks": [
    {
      "mode": "host"
    }
  ],
  "portDefinitions": [
    {
      "labels": {
        "VIP_0": "/${name}:5565"
      },
      "name": "default",
      "protocol": "tcp"
    },
    {
      "labels": {
        "VIP_1": "/${name}:14000"
      },
      "name": "health",
      "protocol": "tcp"
    }
  ]
}
ZZZ

dcos marathon app add ${taskcfgfile}

done
