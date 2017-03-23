
./kafka-topics.sh --list --zookeeper k1:2181
__consumer_offsets
simFile

Source

java -cp target/rt.jar org.jennings.rt.source.tcp.TcpKafka 
Usage: TcpKafka <port-to-listen-on> <broker-list-or-hub-name> <topic> <web-port> (<calc-latency>)

java -cp target/rt.jar org.jennings.rt.source.tcp.TcpKafka 5565 k1:9092 simFile 9000



Sink

java -cp target/rt.jar org.jennings.rt.sink.kafka.KafkaCnt 
Usage: rtsink <broker-list-or-hub-name> <topic> <group-id> <web-port> (<calc-latency>)


java -cp target/rt.jar org.jennings.rt.sink.kafka.KafkaCnt k1:9092 simFile group1 9001


java -cp target/Simulator.jar com.esri.simulator.Tcp 
Usage: Tcp <server> <port> <file> <rate> <numrecords> (<append-time-csv>)

java -cp target/Simulator.jar com.esri.simulator.Tcp localhost 5565 simFile_1000_10s.dat 1000 10000