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
This is the web server for supporting health checks.
 */
package org.jennings.rt.webserver;

import org.jennings.rt.webserver.RootHandler;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;

/**
 *
 * @author david
 */
public class WebServer {

    private final int port;
    
    RootHandler rootHandler;
    
    public void addCnt(long cnt) {
        rootHandler.addCnt(cnt);
    }
    
    public void addRate(double rate) {
        rootHandler.addRate(rate);
    }

    public void addLatency(double latency) {
        rootHandler.addLatency(latency);
    }

    public void setTm(long tm) {
        rootHandler.setTm(tm);
    }    

    public WebServer(int port) {

        this.port = port;

        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/", new RootHandler());

            server.start();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
