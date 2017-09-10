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

package org.jennings.rt;

/*

The idea is to create Java interface to Marathon Rest API

Reference: https://mesosphere.github.io/marathon/docs/rest-api.html

This github project was similar (https://github.com/mohitsoni/marathon-client) hasn't been updated in a couple of years.
There was a fork project https://github.com/xyalan/marathon-cli that was updated a few months ago.
Another fork https://github.com/cloudbees/marathon-client
   For of fork https://github.com/medallia/marathon-client 

 
 */


import java.net.URL;

/**
 *
 * @author david
 */
public class Marathon {
    
    URL url;
    String protocol;
    String host;
    int port;
    

    public Marathon(URL url) {
        this.url = url;
        this.protocol = this.url.getProtocol();
        this.host = this.url.getHost();
        this.port = this.url.getPort();
    }
    
    public Marathon(String url) throws InstantiationException {
        try {
            this.url = new URL(url);
            this.protocol = this.url.getProtocol();
            this.host = this.url.getHost();
            this.port = this.url.getPort();
        } catch (Exception e) {
            this.url = null;
            throw new InstantiationException("Failed to parse url");
            
        }
    }

    public URL getUrl() {
        return url;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    
    
    @Override
    public String toString() {
        System.out.println(this.protocol);
        return "Marathon{" + "url=" + this.url + '}';
    }
    
    // GET /v2/apps: List all running apps
    // Return a List of AppIds (may need group too)
    
    // GET /v2/apps/{appId}: List the app appId
    // Key info about an app 
    
    // POST /v2/apps: Create and start a new app
    // PUT /v2/apps/{appId}: Update or create an app with id appId
    // Need to watch the app to make sure it starts and doesn't fail repeatedly
    
    // DELETE /v2/apps/{appId}: Destroy app appId
    // Remove the app
    
    
    public static void main(String args[]) {
        try {
            Marathon t = new Marathon("https://www.google.com");
            System.out.println(t);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        
    }
    
}
