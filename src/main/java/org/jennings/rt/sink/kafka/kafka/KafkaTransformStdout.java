/*
 * (C) Copyright 2017 David Jennings
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     David Jennings
 */

/*
    Consumes a Kafka Topic and applies a transformation (implementation of Transform Interface) to the line from Kafka then prints to Stdout the transformed line.

    Sample implementation of Transform interface TransformSimFile, TransformGeotagSimFile

 */
package org.jennings.rt.sink.kafka.kafka;


import java.util.Arrays;
import java.util.Properties;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.jennings.rt.MarathonInfo;
import org.jennings.rt.webserver.WebServer;

/**
 *
 * @author david
 */
public class KafkaTransformStdout {
    
    String brokers;
    String topic;
    String group;
    Integer webport;
    Integer timeout;  //ms
    Integer pollingInterval; //ms
    
    
    WebServer server;    
    KafkaConsumer<String, String> consumer;    

    public KafkaTransformStdout(String brokers, String topic, String group, Integer webport) {
        // Default to 5 second timeout and 10ms polling
        this(brokers,topic, group, webport, 5000, 10);
    }


    public KafkaTransformStdout(String brokers, String topic, String group, Integer webport, Integer timeout, Integer pollingInterval) {
        this.brokers = brokers;
        this.topic = topic;
        this.group = group;
        this.webport = webport;
        this.timeout = timeout;
        this.pollingInterval = pollingInterval;
        

        try {
        
            Properties props = new Properties();
            props.put("bootstrap.servers",this.brokers);
            // I should include another parameter for group.id this would allow differenct consumers of same topic
            props.put("group.id", this.group);
            props.put("enable.auto.commit", "true");
            props.put("auto.commit.interval.ms", 1000);
            props.put("auto.offset.reset", "earliest");
            props.put("session.timeout.ms", "30000");
            props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
            props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
            
            consumer = new KafkaConsumer<>(props);
            
            server = new WebServer(this.webport);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    
    
    public void read(String transformId, int everyNthLine) throws Exception {
        
        //Map<String,List<PartitionInfo>> topics = consumer.listTopics();
       
        consumer.subscribe(Arrays.asList(this.topic));

        Transform transformer = null;

        if (transformId.equalsIgnoreCase("NOOP")) {
            transformer = null;
        } else {
            Class<?> clazz = Class.forName(transformId);
            transformer = (Transform) clazz.newInstance();

            if (transformer == null) throw new Exception("Transformmer not defined");

        }


        Long lr = System.currentTimeMillis();
        Long st = System.currentTimeMillis();


        
        Long cnt = 0L;
        
        while (true) {
            ConsumerRecords<String,String> records = consumer.poll(this.pollingInterval);
            // polls every 100ms
            Long ct = System.currentTimeMillis();
            
            if (cnt > 0 && ct - lr > this.timeout) {
                // Longer than 2 seconds reset and output stats
                
                long delta = lr - st;
                double rate = 1000.0 * (double) cnt / (double) delta;
                System.out.println(cnt + "," + rate);
                

                server.addCnt(cnt);
                server.addRate(rate);
                cnt = 0L;

            }
            
            for (ConsumerRecord<String, String> record : records) {   
                lr = System.currentTimeMillis();

                String lineOut = record.value();
                if (transformer != null) {
                    lineOut = transformer.transform(record.value());
                }

                if (cnt%everyNthLine == 0) {    
                    // Only print a message every 1000 times   
                    if (!lineOut.isEmpty()) {
                        System.out.println(cnt + ">> " + record.key() + ":" + lineOut);
                    }
                    
                    //System.out.println(cnt + ">> " + record.key() + ":" + record.value());
                }
                cnt += 1;      
                if (cnt == 1) {
                    st = System.currentTimeMillis();
                }
            }
            //server.addCnt(cnt);
        }
    }

    public static void main(String args[]) throws Exception {
        
        // Example Arguments: a1:9092 simFile group1 simFile 100 14002
        // 
        
        int numArgs = args.length;
        
        if (numArgs != 6 && numArgs != 8) {
            System.err.print("Usage: KafkaTransformStdout <broker-list-or-kafka-app-name> <topic> <group-id> <transform-class-name> <every-nth-line> <web-port> (<timeout-ms> <polling-interval-ms>)\n");
        } else {
            
            
            String brokers = args[0];
            String topic = args[1];
            String groupId = args[2];
            String transformId = args[3];
            Integer everyNthLine = Integer.parseInt(args[4]);
            Integer webport = Integer.parseInt(args[5]);
           
            
            Integer timeout = null;
            Integer pollingInterval = null;
            
            if (numArgs == 8) {
                timeout = Integer.parseInt(args[6]);
                pollingInterval = Integer.parseInt(args[7]);
            }
            
            
            String brokerSplit[] = brokers.split(":");
            
            if (brokerSplit.length == 1) {
                // Try hub name. Name cannot have a ':' and brokers must have it.
                brokers = new MarathonInfo().getBrokers(brokers);
            }   // Otherwise assume it's brokers 
                        
            
            KafkaTransformStdout t = null;
            
            
            System.out.println(brokers);
            System.out.println(topic);
            System.out.println(groupId);
            System.out.println(webport);
            System.out.println(transformId);
            System.out.println(everyNthLine);
            
            switch (numArgs) {
                case 6:
                    t = new KafkaTransformStdout(brokers,topic,groupId,webport);
                    break;
                case 8:
                    t = new KafkaTransformStdout(brokers,topic,groupId,webport,timeout,pollingInterval);
            }
            
            t.read(transformId, everyNthLine);
        }
        
        

        
    }    


    
}
