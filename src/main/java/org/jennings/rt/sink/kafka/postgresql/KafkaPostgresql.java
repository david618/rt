/**
 *
 * Read JSON from Kafka Topic and Write to Postgresql Table
 * - Must be one level JSON with a Lon and Lat Field
 * - The json names are mapped directly to Postgres Field Names
 * - The Postgres Table must already exist
 *
 * David Jennings
 */
package org.jennings.rt.sink.kafka.postgresql;

import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;
import java.util.Set;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.elasticsearch.client.Client;
import org.jennings.rt.webserver.WebServer;
import org.json.JSONObject;

/**
 *
 * @author david
 */
public class KafkaPostgresql {

    final static int INT = 0;
    final static int LNG = 1;
    final static int DBL = 2;
    final static int STR = 3;

    final static int MAXSTRLEN = 100;

    String brokers;
    String topic;
    String group;

    String dbConn;
    String dbtbl;
    String dbuser;
    String dbpass;

    String oidName;
    String geomName;
    String latName;
    String lonName;

    Integer bulk;
    Integer webport;
    WebServer server;

    // Kafka
    KafkaConsumer<String, String> consumer;
    Client client;

    // Postgres
    Connection c = null;
    Statement stmt = null;

    public KafkaPostgresql(Integer webport) {
        try {

            String filename = this.getClass().getName() + ".properties";
            InputStream input = new FileInputStream(filename);

            if (input == null) {
                System.out.println("Sorry, unable to find " + filename);
                return;
            }

            Properties prop = new Properties();
            prop.load(input);

            // Change this app so that fenceURL and fieldName come from Environment or Config file
            this.brokers = prop.getProperty("brokers");
            this.topic = prop.getProperty("topic");
            this.group = prop.getProperty("group");

            this.dbConn = prop.getProperty("dbConn");
            this.dbtbl = prop.getProperty("dbtbl");
            this.dbuser = prop.getProperty("dbuser");
            this.dbpass = prop.getProperty("dbpass");

            this.oidName = prop.getProperty("oidName");
            this.geomName = prop.getProperty("geomName");
            this.latName = prop.getProperty("latName");
            this.lonName = prop.getProperty("lonName");

            this.bulk = Integer.parseInt(prop.getProperty("bulk"));

            this.webport = webport;

            System.out.println("brokers:" + brokers);
            System.out.println("topic:" + topic);
            System.out.println("group:" + group);

            System.out.println("dbConn:" + dbConn);
            System.out.println("dbtbl:" + dbtbl);
            System.out.println("dbuser:" + dbuser);
            System.out.println("dbpass:" + dbpass);

            System.out.println("oidName:" + oidName);
            System.out.println("geomName:" + geomName);
            System.out.println("latName:" + latName);
            System.out.println("lonName:" + lonName);

            System.out.println("bulk:" + bulk);

            System.out.println("webport:" + webport);

            // Setup consumer for Kafka
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

            // Setup Postgres
            c = DriverManager
                    .getConnection("jdbc:postgresql://" + this.dbConn,
                            this.dbuser, this.dbpass);
            c.setAutoCommit(false);

            stmt = c.createStatement();

            // Setup Web Server
            server = new WebServer(webport);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void read() {

        consumer.subscribe(Arrays.asList(this.topic));

        Long lr = System.currentTimeMillis();
        Long st = System.currentTimeMillis();

        Long cnt = 0L;

        int cnt2 = 0;


        String sqlPrefix = "";
        JSONObject json = null;

        HashMap<String, Integer> jsonMap = new HashMap<>();

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

                // Create the Schema
                json = new JSONObject(record.value());               
                
                try {

                    if (sqlPrefix.isEmpty()) {

                        // Create the prefix
                        sqlPrefix = "INSERT INTO " + this.dbtbl + " (" + this.oidName + ",";

                        Set<String> ks = json.keySet();

                        for (String k : ks) {
                            //System.out.println(k);

                            Object val = json.get(k);

                            if (val instanceof Integer) {
                                jsonMap.put(k, INT);
                            } else if (val instanceof Long) {
                                jsonMap.put(k, LNG);
                            } else if (val instanceof Double) {
                                jsonMap.put(k, DBL);
                            } else if (val instanceof String) {
                                jsonMap.put(k, STR);
                            }
                            //System.out.println();
                            sqlPrefix += k + ",";

                        }
                        sqlPrefix += this.geomName + ") VALUES (DEFAULT,";
                        
                        // Print Create Table Statement Example
                        String sql = "CREATE TABLE " + this.dbtbl + " (" + this.oidName + " serial4,";


                        for (String k : ks) {
                            //System.out.println(k);

                            Object val = json.get(k);

                            if (val instanceof Integer) {
                                sql += k + " integer,";
                            } else if (val instanceof Long) {
                                sql += k + " bigint,";;
                            } else if (val instanceof Double) {
                                sql += k + " double precision,";;
                            } else if (val instanceof String) {
                                sql += k + " varchar(" + MAXSTRLEN + "),";;
                            }

                        }

                        sql = sql.substring(0, sql.length() - 1) + ");";
                        System.out.println("Sample Table Create");
                        System.out.println(sql);

                        sql = "SELECT AddGeometryColumn('','" + this.dbtbl + "','" + this.geomName + "',4326,'POINT',2);";
                        System.out.println(sql);                        
                        
                        
                        

                    }

                    // Insert into Postgres
                    String sql = "";

                    sql = sqlPrefix;

                    for (String key : jsonMap.keySet()) {
                        switch (jsonMap.get(key)) {
                            case INT:
                                sql += json.getInt(key) + ",";
                                break;
                            case LNG:
                                sql += json.getLong(key) + ",";
                                break;
                            case DBL:
                                sql += json.getDouble(key) + ",";
                                break;
                            case STR:
                                sql += "'" + json.getString(key).replace("'", "''") + "',";
                                break;
                            default:
                                break;
                        }

                    }

                    //ST_GeomFromText('POINT(-71.060316 48.432044)', 4326)
                    //sql = sql.substring(0,sql.length() - 1) + ");";
                    sql += "ST_GeomFromText('POINT(" + json.getDouble(this.lonName) + " " + json.getDouble(this.latName) + ")', 4326)" + ");";
                    //System.out.println(sql);

                    stmt = c.createStatement();
                    stmt.executeUpdate(sql);

                    cnt += 1;
                    if (cnt == 1) {
                        st = System.currentTimeMillis();
                    }

                    cnt2 += 1;
                    if (cnt2 >= this.bulk) {
                        // Commit to Postgres ??
                        c.commit();                        
                        cnt2 = 0;
                    }
                    
                    lr = System.currentTimeMillis();

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

            try {
                c.commit();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

    }

    public static void main(String[] args) {
        
        int numArgs = args.length;

//        KafkaPostgresql t = new KafkaPostgresql(14000);
//        t.read();
        
        
        if (numArgs == 0) {
            System.err.println("Usages: KafkaPostgresql WebPort");
        } else if (numArgs == 1) {
            KafkaPostgresql t = new KafkaPostgresql(Integer.parseInt(args[0]));
            t.read();
        }

                

    }

}
