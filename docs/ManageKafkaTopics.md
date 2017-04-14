# Manage Kafka Topics

You can manage topics vis DCOS CLI or Kafka tools

## DCOS CLI

<pre>
curl -O https://downloads.dcos.io/binaries/cli/linux/x86-64/0.4.15/dcos
chmod +x dcos
./dcos config set core.dcos_url https://m1
./dcos config set core.ssl_verify false
./dcos auth login
Enter the username "admin"
Password: *****
./dcos package install kafka --cli

</pre>

Add home directory to path
<pre>
vi .bash_profile

Append 
:~
to PATH

. .bash_profile

</pre>

You can now execute commands.

|Description|Command|
|---|---|
|Help on Kafka Topic | dcos kafka --name kafka topic --help |
| List Topics | dcos kafka --name kafka topic list  |
| Descirbe Topic simFile | dcos kafka --name kafka topic describe simFile |
| Set simFile to 2 Partitions | dcos kafka --name kafka topic partitions simFile 2 |
| Delete Topic | dcos kafka --name kafka topic delete simFile |

## Kafka Tools

Download and extract Kafka package
<pre>
curl -O http://www.apache.org/dist/kafka/0.10.1.0/kafka_2.10-0.10.1.0.tgz
tar xvzf kafka_2.10-0.10.1.0.tgz
cd kafka_2.10-0.10.1.0/bin
</pre>

|Description|Command|
|---|---|
|Help | ./kafka-topics.sh |
| List Topics | ./kafka-topics.sh --zookeeper m1:2181/dcos-service-kafka --list  |
| Descirbe Topic simFile | ./kafka-topics.sh --zookeeper m1:2181/dcos-service-kafka --describe --topic simFile |
| Set simFile to 2 Partitions | ./kafka-topics.sh --zookeeper m1:2181/dcos-service-kafka --alter --topic simFile --partitions 2 |
| Delete Topic | ./kafka-topics.sh --zookeeper m1:2181/dcos-service-kafka --delete --topic simFile  |

