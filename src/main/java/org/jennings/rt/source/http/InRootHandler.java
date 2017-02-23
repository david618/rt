/*
This class is used in Marathon to setup response for Health Check.

Additional code could be added to check rtsource and return errors so Marathon can restart if needed.

 */
package org.jennings.rt.source.http;

import org.jennings.rt.webserver.WebServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.RecordMetadata;

/**
 *
 * @author david
 */
public class InRootHandler implements HttpHandler {

    static ArrayList<Long> cnts = new ArrayList<>();
    static ArrayList<Double> rates = new ArrayList<>();
    static ArrayList<Double> latencies = new ArrayList<>();
    static long tm = System.currentTimeMillis();

    private Producer<String, String> producer;
    private String topic;
    private WebServer server;
    private boolean calcLatency;
    private Boolean calcLatencyThisRun;

    private long cnt;
    private Long sumLatencies;

    private Timer timer = new Timer();
    private long lr = System.currentTimeMillis();
    private long st = System.currentTimeMillis();

    public InRootHandler(Producer<String, String> producer, String topic, WebServer server, boolean calcLatency) {
        this.producer = producer;
        this.topic = topic;
        this.server = server;
        this.calcLatency = calcLatency;
        this.cnt = 0L;
    }

    private void writeLineKafka(String line) {

        UUID uuid = UUID.randomUUID();
        try {
            ProducerRecord<String, String> record = new ProducerRecord<String, String>(this.topic, uuid.toString(), line);
            producer.send(record,
                    new Callback() {
                public void onCompletion(RecordMetadata metadata, Exception e) {
                    if (e != null) {
                        e.printStackTrace();
                    }
                    //System.out.println("The offset of the record we just sent is: " + metadata.offset());
                }
            });


//            producer.send(new ProducerRecord<String, String>(this.topic, uuid.toString(), line));

        } catch (Exception e) {
            e.printStackTrace();
        }

        this.cnt += 1;

        lr = System.currentTimeMillis();

        if (cnt == 1L) {

            calcLatencyThisRun = calcLatency;
            // Start a timer
            st = System.currentTimeMillis();
            sumLatencies = 0L;
            timer.schedule(new TimerTask() {
                @Override
                public void run() {

                    long delta = System.currentTimeMillis() - lr;
                    //System.out.println(delta);

                    if (delta > 5000) {
                        // Calculate Rate
                        double rate = -1.0;
                        if (lr - st > 0) {
                            rate = (double) cnt / (lr - st) * 1000.0;
                        }

                        double avgLat = -1.0;
                        if (cnt > 0) {
                            avgLat = (double) sumLatencies / (double) cnt;  //ms
                        }

                        if (calcLatencyThisRun) {
                            //System.out.println(cnt + "," + rcvRate + "," + avgLatency);
                            System.out.format("%d , %.0f , %.3f\n", cnt, rate, avgLat);
                        } else {
                            //System.out.println(cnt + "," + rcvRate);
                            System.out.format("%d , %.0f\n", cnt, rate);
                        }

                        server.addLatency(avgLat);
                        server.addRate(rate);
                        server.addCnt(cnt);
                        server.setTm(lr);

                        timer.cancel();
                        timer = new Timer();
                        cnt = 0L;
                        sumLatencies = 0L;

                    }
                }
            }, 1000, 1000);

        }

        if (calcLatencyThisRun == null) {
            calcLatencyThisRun = false;
        }

        if (calcLatencyThisRun) {

            try {
                // Assumes csv and that input also has time in nanoSeconds from epoch
                long tsent = Long.parseLong(line.substring(line.lastIndexOf(",") + 1));

                long trcvd = System.currentTimeMillis();

                sumLatencies += (trcvd - tsent);
                // If trcvd appended then latency will be measured between Kafka write/read
                //line += String.valueOf(trcvd);
            } catch (Exception e) {
                System.out.println("For Latency Calculations last field in CSV must be milliseconds from Epoch");
                calcLatencyThisRun = false;
            }

        }

    }

    @Override
    public void handle(HttpExchange he) throws IOException {
        String response = "";

        JSONObject obj = new JSONObject();
        try {

            String uriPath = he.getRequestURI().toString();

            if (uriPath.equalsIgnoreCase("/")) {

                if (he.getRequestMethod().equalsIgnoreCase("POST")) {
                    InputStreamReader isr = new InputStreamReader(he.getRequestBody(), "utf-8");
                    BufferedReader br = new BufferedReader(isr);
                    String line = br.readLine();
                    writeLineKafka(line);

                } else {
                    throw new Exception("GET not supported. POST events to the server.");
                }

                obj.put("ok", true);
            } else {
                obj.put("error", "Unsupported URI");
            }
            response = obj.toString();
        } catch (Exception e) {
            response = "\"error\":\"" + e.getMessage() + "\"";
            e.printStackTrace();
        }

        he.sendResponseHeaders(200, response.length());
        OutputStream os = he.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

}
