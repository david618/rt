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

TcpServerKafka is the listener for TcpKafka.  

Instance of this class starts when client connects.

Listens on TCP port for messages and writes them to Kafka.

*/
package org.jennings.rt.source.tcp;

import org.jennings.rt.webserver.WebServer;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

/**
 *
 * @author david
 */
public class TcpServerKafka extends Thread {
    private Socket socket = null;
    private Producer<String, String> producer;
    private String topic;
    private WebServer server;
    private boolean calcLatency;
    
    public TcpServerKafka(Socket socket, Producer<String, String> producer, String topic, WebServer server, boolean calcLatency) {
        this.socket = socket;
        this.producer = producer;
        this.topic = topic;
        this.server = server;
        this.calcLatency = calcLatency;
    }
    
    @Override
    public void run() {
        try {
            
            BufferedReader in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
            
            String inputLine = "";
            
            // Read lines from Socket forever
            
            LocalDateTime st = LocalDateTime.now();
            
            Integer cnt = 0;
            Long sumLatencies = 0L;

            while ((inputLine = in.readLine()) != null) {
                //System.out.println(inputLine);

                UUID uuid = UUID.randomUUID();

                String line = inputLine;

                if (this.calcLatency) {

                    try {
                        // Assumes csv and that input also has time in nanoSeconds from epoch
                        long tsent = Long.parseLong(line.substring(line.lastIndexOf(",") + 1));

                        long trcvd = System.currentTimeMillis();

                        sumLatencies += (trcvd - tsent);
                        // If trcvd appended then latency will be measured between Kafka write/read
                        //line += String.valueOf(trcvd);
                    } catch (Exception e) {
                        System.out.println("For Latency Calculations last field in CSV must be milliseconds from Epoch");
                        this.calcLatency = false;
                    }


                }

                try {
                    producer.send(new ProducerRecord<String,String>(this.topic, uuid.toString(),line));
                } catch (Exception e) {
                    e.printStackTrace();
                }

                cnt += 1;
                if (cnt == 1) st = LocalDateTime.now();
            }

            Double rcvRate = 0.0;
            Double avgLatency = 0.0;

            if (st != null ) {
                LocalDateTime et = LocalDateTime.now();

                Duration delta = Duration.between(st, et);

                Double elapsedSeconds = (double) delta.getSeconds() + delta.getNano() / 1000000000.0;

                rcvRate = (double) cnt / elapsedSeconds;

                avgLatency = (double) sumLatencies / (double) cnt  ;  //ms
            }                

            long tm = System.currentTimeMillis();

            server.addCnt(cnt);
            server.addRate(rcvRate);
            server.addLatency(avgLatency);
            server.setTm(tm);

            if (this.calcLatency) {
                //System.out.println(cnt + "," + rcvRate + "," + avgLatency);
                System.out.format("%d , %.0f , %.3f\n", cnt,rcvRate,avgLatency);
            } else {
                //System.out.println(cnt + "," + rcvRate);
                System.out.format("%d , %.0f\n", cnt,rcvRate);
            }


            this.socket.close();
            
        } catch (Exception e) {
            e.printStackTrace();
        }        

    }
    
}
