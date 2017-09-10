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

TcpNoOp starts instances of TcpServerNoOp as clients connect.


*/
package org.jennings.rt.source.tcpnoop;

import org.jennings.rt.webserver.WebServer;
import java.net.ServerSocket;
import java.net.Socket;

/**
 *
 * @author david
 */
public class TcpNoOp {
   

    public TcpNoOp(Integer port, Integer webport) {

        try {

            WebServer server = new WebServer(webport);

            ServerSocket ss = new ServerSocket(port);
            
            while (true) {
                Socket cs = ss.accept();
                TcpServerNoOp ts = new TcpServerNoOp(cs);
                ts.start();
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }
    
    
    public static void main(String args[]) {
        
        //Command Line Options Example: 5565 9000
        
        if (args.length != 2) {
            System.err.print("Usage: TcpNoOp <port-to-listen-on> <web-port>\n");
        } else {
            TcpNoOp t = new TcpNoOp(Integer.parseInt(args[0]), Integer.parseInt(args[1]));

        }
                
    }
}
