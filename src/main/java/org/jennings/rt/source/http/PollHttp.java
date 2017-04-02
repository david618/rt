/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jennings.rt.source.http;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 *
 * @author david
 */
public class PollHttp {

    class Poll extends TimerTask {

        @Override
        public void run() {
            if (cnt > 0) {
                i += 1;
            }
            

            try {

                //System.out.println(System.currentTimeMillis());

                SSLContext sslContext = SSLContext.getInstance("SSL");

                sslContext.init(null, new TrustManager[]{new X509TrustManager() {
                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        System.out.println("getAcceptedIssuers =============");
                        return null;
                    }

                    @Override
                    public void checkClientTrusted(X509Certificate[] certs,
                            String authType) {
                        System.out.println("checkClientTrusted =============");
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] certs,
                            String authType) {
                        System.out.println("checkServerTrusted =============");
                    }
                }}, new SecureRandom());

                CloseableHttpClient httpclient = HttpClients
                        .custom()
                        .setSSLContext(sslContext)
                        .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                        .build();

                HttpGet request = new HttpGet(url);
                CloseableHttpResponse response = httpclient.execute(request);
                BufferedReader rd = new BufferedReader(
                        new InputStreamReader(response.getEntity().getContent()));

                Header contentType = response.getEntity().getContentType();
                String ct = contentType.getValue().split(";")[0];

                UUID uuid = UUID.randomUUID();

                int responseCode = response.getStatusLine().getStatusCode();

                int n = 0;
                if (responseCode != 200) {
                    System.out.println("Problem connecting to url");
                } else {
                    
                    String line;
                    if (ct.equalsIgnoreCase("application/json")) {
                        StringBuffer result = new StringBuffer();
                        while ((line = rd.readLine()) != null) {
                            result.append(line);
                        }

                        String data = result.toString();

                        Object json = new JSONTokener(data).nextValue();
                        if (json instanceof JSONObject) {
                            // Handle one JSONObject
                            //System.out.println("JSONObject");
                            n += 1;
                            producer.send(new ProducerRecord<String, String>(topic, uuid.toString(), result.toString()));
                        } else if (json instanceof JSONArray) {
                            // Handle as JSONArray
                            //System.out.println("JSONArray");
                            JSONArray arr = new JSONArray(result.toString());
                            for (int i = 0; i < arr.length(); i++) {
                                n += 1;
                                producer.send(new ProducerRecord<String, String>(topic, uuid.toString(), arr.getJSONObject(i).toString()));
                            }
                        }

                    } else {
                        // Assume it's delimited text
                        //System.out.println("Text");

                        while ((line = rd.readLine()) != null) {
                            //System.out.println(line);
                            n += 1;
                            producer.send(new ProducerRecord<String, String>(topic, uuid.toString(), line));
                        }

                    }

                }

                System.out.println(System.currentTimeMillis() + "," + n);
                request.abort();
                response.close();
                
                
            } catch (Exception e) {

                e.printStackTrace();
            }

            if (i >= cnt && cnt > 0) {
                timer.cancel();
            }
        }
    }

    Timer timer;
    Integer cnt;
    Integer i;
    String url;
    String brokers;
    String topic;
    private Producer<String, String> producer;

    public PollHttp(int cnt, long ms, String url, String brokers, String topic) {

        this.cnt = cnt;
        this.i = 0;
        this.url = url;
        this.brokers = brokers;
        this.topic = topic;

        try {
            Properties props = new Properties();
            props.put("bootstrap.servers", brokers);
            props.put("client.id", PollHttp.class.getName());
            props.put("acks", "1");
            props.put("retries", 0);
            props.put("batch.size", 16384);
            props.put("linger.ms", 1);
            props.put("buffer.memory", 8192000);
            props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
            props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
            /* Addin Simple Partioner didn't help */
            //props.put("partitioner.class", SimplePartitioner.class.getCanonicalName());            

            producer = new KafkaProducer<>(props);

        } catch (Exception e) {
            e.printStackTrace();
        }

        timer = new Timer();
        timer.schedule(new Poll(), 0, ms);

    }

    public static void main(String[] args) {
//        new PollHttp(20, 1000, "http://localhost:8080/websats/satellites?f=json&n=20", "k1:9092", "satellites");
        int numargs = args.length;
        if (numargs != 5) {
            System.err.print("Usage: PollHttp numberTimes periodMS url brokers topic\n");
        } else {
            new PollHttp(Integer.parseInt(args[0]), Integer.parseInt(args[1]), args[2], args[3], args[4]);
        }

    }

}
