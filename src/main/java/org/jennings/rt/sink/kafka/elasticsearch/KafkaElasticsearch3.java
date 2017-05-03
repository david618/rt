/*
 Consume a Kafka topic and write lines to Elasticsearch.  

Assumes the elements from Kafka are Json Strings suitable for loading into Elasticsearch.

This version uses Elasticsearch rest api

David Jennings

 */
package org.jennings.rt.sink.kafka.elasticsearch;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.HttpClientBuilder;

import org.apache.kafka.common.PartitionInfo;
import org.elasticsearch.client.Client;
import org.jennings.rt.MarathonInfo;
import org.jennings.rt.webserver.WebServer;
import org.json.JSONObject;

/**
 * Created by david on 8/20/2016.
 */
public class KafkaElasticsearch3 {

    // Parsting Text
    static final Pattern PATTERN = Pattern.compile("(([^\"][^,]*)|\"([^\"]*)\"),?");

    // Elasticsearch
    private final String USER_AGENT = "Mozilla/5.0";
    private HttpClient httpClient;
    private HttpPost httpPost;

    private String strURL;  // http://data.sats-sat03.l4lb.thisdcos.directory:9200/index/type
    private Integer esbulk;

    // Kafka 
    KafkaConsumer<String, String> consumer;
    Client client;
    String brokers;
    String topic;
    String group;

    // Web Server For Task Health/Counts
    WebServer server;

    public KafkaElasticsearch3(String brokers, String topic, String group, String strURL, Integer esbulk, Integer webport, String username, String password) {

        try {

            this.brokers = brokers;
            this.topic = topic;
            this.group = group;

            Properties props = new Properties();
            props.put("bootstrap.servers", this.brokers);
            props.put("group.id", this.group);
            props.put("enable.auto.commit", "true");
            props.put("auto.commit.interval.ms", 1000);
            props.put("auto.offset.reset", "earliest");
            props.put("session.timeout.ms", "30000");
            props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
            props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

            consumer = new KafkaConsumer<>(props);

            if (strURL.endsWith("/")) {
                this.strURL = strURL + "_bulk";
            } else {
                this.strURL = strURL + "/_bulk";
            }
            
            if (!strURL.startsWith("http")) {
                // Add http if no prefix is provided
                this.strURL = "http://" + this.strURL;
            }
            

            this.esbulk = esbulk;

            if (esbulk < 0) {
                esbulk = 0;
            }
            
            CredentialsProvider provider = new BasicCredentialsProvider();
            UsernamePasswordCredentials credentials
                    = new UsernamePasswordCredentials(username, password);
            provider.setCredentials(AuthScope.ANY, credentials);
            
            
            httpClient = HttpClientBuilder.create()
                    .setDefaultCredentialsProvider(provider)
                    .build();

            httpPost = new HttpPost(this.strURL);

            server = new WebServer(webport);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void postLine(String data) throws Exception {

        StringEntity postingString = new StringEntity(data);

        //System.out.println(data);
        httpPost.setEntity(postingString);
        //httpPost.setHeader("Content-type","plain/text");
        httpPost.setHeader("Content-type", "application/json");

        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        String resp = httpClient.execute(httpPost, responseHandler);

        JSONObject jsonResp = new JSONObject(resp);
        //System.out.println(jsonResp);
        //httpPost.releaseConnection();
    }

    public void read() {

        Map<String, List<PartitionInfo>> topics = consumer.listTopics();

        consumer.subscribe(Arrays.asList(this.topic));

        Long lr = System.currentTimeMillis();
        Long st = System.currentTimeMillis();

        Long cnt = 0L;

        int cnt2 = 0;

        String line = "";

        //BulkRequestBuilder bulkRequest = client.prepareBulk();
        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(10);
            // polls every 10ms
            Long ct = System.currentTimeMillis();

            if (cnt > 0 && ct - lr > 5000) {

                // Longer than 5 seconds reset and output stats
                long delta = lr - st;
                double rate = 1000.0 * (double) cnt / (double) delta;
                System.out.println(cnt + "," + rate);

                server.addRate(rate);
                server.setTm(System.currentTimeMillis());
                server.addCnt(cnt);
                cnt = 0L;
            }

            for (ConsumerRecord<String, String> record : records) {
                

                try {
                    line += "{\"index\": {}}\n";
                    line += record.value() + "\n";

                    cnt += 1;
                    if (cnt == 1) {
                        st = System.currentTimeMillis();
                    }

                    cnt2 += 1;
                    if (cnt2 >= esbulk) {
                        postLine(line);
                        line = "";
                        lr = System.currentTimeMillis();
                        cnt2 = 0;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
            // Post any remaining data
            try {
                if (!line.isEmpty()) {                    
                    postLine(line);
                    line = "";
                    lr = System.currentTimeMillis();
                    
                }
                
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    public static void main(String args[]) throws Exception {
        // Example Arguments: a1:9092 simFile2 group3 http://data.sats-sat03.l4lb.thisdcos.directory:9200/simFile/simFileType 1000 16001
        // Example Arguments: hub2 simFile2 group3 elasticsearch - sink simfile2 1000

        // You can specify the Marthon name of Elasticsearch and the app looks up ips of tasks; then - for clusterName to look that up too

//        KafkaElasticsearch3 t = new KafkaElasticsearch3("kafka:9092", "simFile", "group1", "http://e5:9200/test1/simFile", 1000, 18000);
//        t.read();

        int numArgs = args.length;

        if (numArgs != 6 && numArgs != 8) {
            System.err.print("Usage: KafkaElasticsearch3 <broker-list> <topic> <group-id> <elastic-url-to-type-index> <es-bulk> <web-port> (<username> <password>)\n");
        } else {
            String brokers = args[0];
            String topic = args[1];
            String groupId = args[2];
            String esURL = args[3];
            Integer bulk = Integer.parseInt(args[4]);
            Integer webport = Integer.parseInt(args[5]);

            String username = "";
            String password = "";
            if (numArgs == 8) {
                username = args[6];
                password = args[7];
            }
             

            String brokerSplit[] = brokers.split(":");
            if (brokerSplit.length == 1) {
                // Try hub name. Name cannot have a ':' and brokers must have it.
                brokers = new MarathonInfo().getBrokers(brokers);
            }   // Otherwise assume it's brokers


            System.out.println(brokers);
            System.out.println(topic);
            System.out.println(groupId);
            System.out.println(esURL);
            System.out.println(bulk);
            System.out.println(webport);

            KafkaElasticsearch3 t = new KafkaElasticsearch3(brokers, topic, groupId, esURL, bulk, webport, username, password);
            t.read();

        }
    }
}
