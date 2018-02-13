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

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Arrays;
import java.util.Properties;
import org.jennings.rt.MarathonInfo;
import org.jennings.rt.webserver.WebServer;

/**
 *
 * @author david
 */
public class KafkaTransformKafka {

    String brokersIn;
    String brokersOut;
    String topicIn;
    String groupId;
    String topicOut;
    Integer webport;
    Integer timeout;  //ms
    Integer pollingInterval; //ms

    WebServer server;
    KafkaConsumer<String, String> consumer;

    Producer<String, String> producer;

    public KafkaTransformKafka(String brokersIn, String topicIn, String groupId, String brokersOut, String topicOut, Integer webport, Integer timeout, Integer pollingInterval) {
        this.brokersIn = brokersIn;
        this.topicIn = topicIn;
        this.groupId = groupId;
        this.brokersOut = brokersOut;
        this.topicOut = topicOut;
        this.webport = webport;
        this.timeout = timeout;
        this.pollingInterval = pollingInterval;

        try {

            Properties propsCons = new Properties();
            propsCons.put("bootstrap.servers", this.brokersIn);
            // I should include another parameter for groupId.id this would allow differenct consumers of same topicIn
            propsCons.put("group.id", this.groupId);
            propsCons.put("enable.auto.commit", "true");
            propsCons.put("auto.commit.interval.ms", 1000);
            propsCons.put("auto.offset.reset", "latest");
            propsCons.put("session.timeout.ms", "30000");
            propsCons.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
            propsCons.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

            consumer = new KafkaConsumer<>(propsCons);

            Properties propsProd = new Properties();
            propsProd.put("bootstrap.servers", this.brokersOut);
            propsProd.put("client.id", KafkaTransformKafka.class.getName());
            propsProd.put("acks", "1");
            propsProd.put("retries", 0);
            propsProd.put("batch.size", 16384);
            propsProd.put("linger.ms", 1);
            propsProd.put("buffer.memory", 8192000);
            propsProd.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
            propsProd.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

            producer = new KafkaProducer<>(propsProd);

            server = new WebServer(this.webport);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void read(String transformId) throws Exception {

        //Map<String,List<PartitionInfo>> topics = consumer.listTopics();
        consumer.subscribe(Arrays.asList(this.topicIn));

        Transform transformer = null;

        if (!transformId.equalsIgnoreCase("NoOp")) {
            Class<?> clazz = Class.forName(transformId);
            transformer = (Transform) clazz.newInstance();

            if (transformer == null) {
                throw new Exception("Transformmer not defined");
            }

        }

        Long lr = System.currentTimeMillis();
        Long st = System.currentTimeMillis();

        Long cnt = 0L;
        String lineOut;

        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(this.pollingInterval);
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

                if (transformer == null) {
                    lineOut = record.value();
                } else {
                    lineOut = transformer.transform(record.value());
                }

                // Only print a message every 1000 times
                if (!lineOut.isEmpty()) {
                    //System.out.println(cnt + ">> " + record.key() + ":" + lineOut);
                    producer.send(new ProducerRecord<>(this.topicOut, record.key(), lineOut));

                }

                //System.out.println(cnt + ">> " + record.key() + ":" + record.value());
                cnt += 1;
                if (cnt == 1) {
                    st = System.currentTimeMillis();
                }
            }

        }
    }

    public static void main(String args[]) throws Exception {

        // Example Arguments: a1:9092 simFile group1 simFile 100 14002
        int numArgs = args.length;

        if (numArgs != 7 && numArgs != 8 && numArgs != 9) {
            System.err.print("Usage: KafkaTransformKafka <brokersIn> <topicIn> <groupId> <transform-class-name> <brokersOut> <topicOut> <web-port> (<timeout-ms> <polling-interval-ms>)\n");
            System.err.print("Use trasform-class-name NoOp to just copy one topic as is to another");
            System.err.print("Default timeout-ms = 5000");
            System.err.print("Default polling-interval-ms = 10");

        } else {

            String brokersIn = args[0];
            String topicIn = args[1];
            String groupId = args[2];
            String transformId = args[3];
            String brokersOut = args[4];
            String topicOut = args[5];
            Integer webport = Integer.parseInt(args[6]);

            Integer timeout = 5000;
            Integer pollingInterval = 10;

            if (numArgs >= 8) {
                timeout = Integer.parseInt(args[7]);
            }

            if (numArgs == 9) {
                pollingInterval = Integer.parseInt(args[8]);
            }

            String brokerSplit[] = brokersIn.split(":");

            if (brokerSplit.length == 1) {
                // Since brokersIn doesn't have colon assume it's a Marathon Name (e.g. kafka)
                brokersIn = new MarathonInfo().getBrokers(brokersIn);
            }   

            brokerSplit = brokersOut.split(":");

            if (brokerSplit.length == 1) {
                // Since brokersOut doesn't have colon assume it's a Marathon Name (e.g. kafka)
                brokersOut = new MarathonInfo().getBrokers(brokersOut);
            }   

            KafkaTransformKafka t = null;

            System.out.println(brokersIn);
            System.out.println(topicIn);
            System.out.println(groupId);
            System.out.println(brokersOut);
            System.out.println(topicOut);
            System.out.println(webport);
            System.out.println(transformId);

            t = new KafkaTransformKafka(brokersIn, topicIn, groupId, brokersOut, topicOut, webport, timeout, pollingInterval);

            t.read(transformId);
        }

    }

}
