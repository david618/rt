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

Reads planes delimited text from Kafka; geotags with country; writes json to Kafka

 */
package org.jennings.rt.sink.kafka.kafka;

import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.MapGeometry;
import com.esri.core.geometry.OperatorExportToJson;
import com.esri.core.geometry.OperatorImportFromJson;
import com.esri.core.geometry.OperatorWithin;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.SpatialReference;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.jennings.rt.webserver.WebServer;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author david
 */
public class TransformGeotagPlanes implements Transform {

    String brokers;
    String topic;
    String group;
    String fenceUrl;
    String fieldName;
    boolean filter;
    Integer webport;
    WebServer server;

    static final Pattern PATTERN = Pattern.compile("(([^\"][^,]*)|\"([^\"]*)\"),?");

    KafkaConsumer<String, String> consumer;

    private HashMap<String, Geometry> polysMap = new HashMap<>();

    Properties prop = new Properties();
    InputStream input = null;
    
    public TransformGeotagPlanes() throws Exception {

        // Consider option of loading properties from zookeeper instead of file 
        // or pass as environment variables 
        // or via a URL that is attached to the app of the Marathon App.
        
        // For dynamic fences each client would need to have a interface that would accept changes of fences
        
        String filename = this.getClass().getName() + ".properties";
        input = new FileInputStream(filename);
        ///input = TransformGeotagSimFile.class.getClassLoader().getResourceAsStream(filename);
        if(input==null){
            System.out.println("Sorry, unable to find " + filename);
            return;
        }

        //load a properties file from class path, inside static method
        prop.load(input);

        // Change this app so that fenceURL and fieldName come from Environment or Config file

        this.fenceUrl = prop.getProperty("fenceUrl");
        this.fieldName = prop.getProperty("fieldName");
        this.filter = prop.getProperty("filter").equalsIgnoreCase("true");


        System.out.println(this.fenceUrl);
        System.out.println(this.fieldName);

        try {        
            loadFences(this.fenceUrl, this.fieldName);

        } catch (Exception e) {
            e.printStackTrace();
        }


    }

   
    public void showFences() {        
        try {            
            for (Map.Entry pair : this.polysMap.entrySet()) {
                System.out.println(pair.getKey() + " = " + pair.getValue());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }
  
    
    private String getNamesWithin(Geometry pt) {
        // Given a point check all the fences and return comma sep string of airports
        String fields = "";
        SpatialReference sr = SpatialReference.create(4326);
        
        try {
            Iterator<Map.Entry<String,Geometry>> it = this.polysMap.entrySet().iterator();
            
            while (it.hasNext()) {

                Map.Entry<String,Geometry> pair = (Map.Entry<String,Geometry>) it.next();
                //System.out.println(pair.getKey() + " = " + pair.getValue());
                
                
                if (OperatorWithin.local().execute(pt, pair.getValue(), sr, null)) { 
                    if (fields.isEmpty()) {
                        fields += pair.getKey();
                    } else {
                        fields += "," + pair.getKey();
                    }

                }                
                
            }            
            
        } catch (Exception e ) {
            e.printStackTrace();
        }
       
        return fields;
               
    }
    

    private boolean loadFences(String url, String fieldName) {
        // Hard coded for now to read fences from a local file

        //String fieldName = "iata_faa";

        boolean isLoaded = false;
        
        
        //String url = "http://m1.trinity.dev/airports1000FS.json";
        InputStream is = null;
        BufferedReader rd = null;
        
                        
        try {
           
            is = new URL(url).openStream();
            rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            StringBuilder sb = new StringBuilder();
            int cp;
            while ((cp = rd.read()) != -1) {
                sb.append((char) cp);
            }
            String jsonText = sb.toString();
            
            JSONObject root = new JSONObject(jsonText);
                                   
            Iterator<String> keyInterator = root.keys();
            
            while (keyInterator.hasNext()) {
                System.out.println(keyInterator.next());
            }
            
            if (root.has("features")) {
                
                JSONArray features = root.getJSONArray("features");
                int cnt = features.length();
                System.out.println("File has " + cnt + " Features");
                int i = 0;
                
                
                while (i < cnt) {

                      
                    JSONObject geom = features.getJSONObject(i).getJSONObject("geometry");
                    
                    
                    MapGeometry poly =  OperatorImportFromJson.local().execute(Geometry.Type.Polygon, geom);
                    
                    String attr = features.getJSONObject(i).getJSONObject("attributes").getString(fieldName);
                    
                    //polys.add(poly);
                    this.polysMap.put(attr, poly.getGeometry());
                    

                    i += 1;
                    
                }
                

                while (keyInterator.hasNext()) {
                    System.out.println(keyInterator.next());
                }
                
            }

        } catch (Exception e) {
                        
            e.printStackTrace();
                                    
        } finally {
            //try { fr.close(); } catch (Exception e) { /* ok to ignore*/ }
            try { is.close(); } catch (Exception e) { /* ok to ignore*/ }
            try { rd.close(); } catch (Exception e) { /* ok to ignore*/ }
        }

        return isLoaded;
    }

    
    @Override
    public String transform(String line) {
        String lineOut = "";

        // Load mapping here could come from config file or from web service
        try {

            ArrayList<String> vals = new ArrayList<String>();
            Matcher matcher = PATTERN.matcher(line);
            int i = 0;
            while (matcher.find()) {
                if (matcher.group(2) != null) {
                    //System.out.print(matcher.group(2) + "|");
                    vals.add(i, matcher.group(2));
                } else if (matcher.group(3) != null) {
                    //System.out.print(matcher.group(3) + "|");
                    vals.add(i, matcher.group(3));
                }
                i += 1;
            }

            //0,1506911657612,230.15,2497.68,41.29,1,"Wright-Patterson Air Force Base","Mielec Airport",-1,-14.95027,59.27356
            Integer id = Integer.parseInt(vals.get(0)); 
            Long ts = Long.parseLong(vals.get(1));
            Double spd = Double.parseDouble(vals.get(2));
            Double dst = Double.parseDouble(vals.get(3));
            Double brg = Double.parseDouble(vals.get(4));
            Integer rid = Integer.parseInt(vals.get(5));             
            String orig = vals.get(6);
            String dest = vals.get(7);
            Integer s2d = Integer.parseInt(vals.get(8));                        
            Double lon = Double.parseDouble(vals.get(9));
            Double lat = Double.parseDouble(vals.get(10));
                        

            JSONObject attrJson = new JSONObject();
            attrJson.put("id", id);
            attrJson.put("ts", ts);
            attrJson.put("spd", spd);
            attrJson.put("dst", dst);
            attrJson.put("brg", brg);
            attrJson.put("rid", rid);
            attrJson.put("orig", orig);
            attrJson.put("dest", dest);
            attrJson.put("s2d", s2d);
            attrJson.put("lon", lon);
            attrJson.put("lat", lat);

            // Turn lon/lat into GeoJson

            Point pt = new Point(lon, lat);

            SpatialReference sr = SpatialReference.create(4326);

            String geomJsonString = OperatorExportToJson.local().execute(sr, pt);
            JSONObject geomJson = new JSONObject(geomJsonString);
            
            String geoTag = getNamesWithin(pt);
            
            if (filter) {
                if (geoTag.isEmpty()) {
                    return "";
                }
            }
            
            attrJson.put("geotag", geoTag);

            // Combine the attributes and the geom
            JSONObject combined = new JSONObject();
            combined.put("attr", attrJson);
            combined.put("geom", geomJson);

            lineOut = combined.toString();
            

        } catch (Exception e) {
            lineOut = "{\"error\":\"" + e.getMessage() + "\"";
        }

        return lineOut;    }
    
    
    public static void main(String args[]) throws Exception {
        
        
        TransformGeotagPlanes t = new TransformGeotagPlanes();
        
        FileReader fr = new FileReader("/home/david/testfolder/plane00001");
        BufferedReader br = new BufferedReader(fr);
        
        
        
        int n = 0;
        while (n < 10000 && br.ready() ) {
            String line = br.readLine();
            
            
            String outLine = t.transform(line);
            if (!outLine.isEmpty()) {
                System.out.println(line);
                System.out.println(outLine);
            }
            
            n++;
        }
        
        br.close();
        fr.close();

    }    



}
