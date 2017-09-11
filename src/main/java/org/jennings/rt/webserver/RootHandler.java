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
This class is used in Marathon to setup response for Health Check.

Additional code could be added to check rtsource and return errors so Marathon can restart if needed.

*/
package org.jennings.rt.webserver;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import org.json.JSONObject;

/**
 *
 * @author david
 */
public class RootHandler implements HttpHandler {


    static ArrayList<Long> cnts = new ArrayList<>();
    static ArrayList<Double> rates = new ArrayList<>();
    static ArrayList<Double> latencies = new ArrayList<>();
    static long tm = System.currentTimeMillis();

    public static void reset() {
        cnts = new ArrayList<>();
        rates = new ArrayList<>();
        latencies = new ArrayList<>();
        tm = System.currentTimeMillis();
    }
    
    public void addCnt(long cnt) {
        cnts.add(cnt);
    }

    public void addRate(double rate) {
        rates.add(rate);
    }

    public void addLatency(double latency) {
        latencies.add(latency);
    }

    public void setTm(long tm) {
        RootHandler.tm = tm;
    }  
    
    @Override
    public void handle(HttpExchange he) throws IOException {
        String response = "";
        
        JSONObject obj = new JSONObject();
        try {
            
            String uriPath = he.getRequestURI().toString();
            
            if (uriPath.equalsIgnoreCase("/count") || uriPath.equalsIgnoreCase("/count/")) {
                // Return count
                obj.put("tm", tm);
                obj.put("counts", cnts.toArray());    
                obj.put("rates", rates.toArray());
                obj.put("latencies", latencies.toArray());
            } else if (uriPath.equalsIgnoreCase("/reset") || uriPath.equalsIgnoreCase("/reset/")) {
                // Reset counts
                reset();
                obj.put("done", true);     
            } else if (uriPath.equalsIgnoreCase("/")) {
                // 
                // Add additional code for health check
                obj.put("healthy", true);                        
            } else {
                obj.put("error","Unsupported URI");
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
