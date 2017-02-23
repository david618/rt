package org.jennings.rt;

/*

This could be used to manually partition inputs to spread load accross brokers.

From my initial test the default partitioner works pretty well at least at 0.10.0.0.

*/


import java.util.Map;
import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.common.Cluster;

public class SimplePartitioner implements Partitioner {

    @Override
    public int partition(String string, Object o, byte[] bytes, Object o1, byte[] bytes1, Cluster clstr) {
        int numParts = clstr.partitionCountForTopic("simFile");
        return Math.abs(o.hashCode()) % numParts;
    }

    @Override
    public void close() {
        
    }

    @Override
    public void configure(Map<String, ?> map) {
        
    }

}
