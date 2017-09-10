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
TcpServerNoOp is the listener for TcpNoOp.  Instance of this class starts when client connects.

Listens on TCP port for messages and counts them.
*/
package org.jennings.rt.source.tcpnoop;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 *
 * @author david
 */
public class TcpServerNoOp extends Thread {
    private Socket socket = null;

    
    public TcpServerNoOp(Socket socket) {
        this.socket = socket;

    }
    
    @Override
    public void run() {
        try {
            
            BufferedReader in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
            
            String inputLine = "";
            
            // Read lines from Socket forever
            
            LocalDateTime st = LocalDateTime.now();
            

            Integer cnt = 0;
            while ((inputLine = in.readLine()) != null) {
                //System.out.println(inputLine);

                UUID uuid = UUID.randomUUID();

                cnt += 1;
                if (cnt == 1) st = LocalDateTime.now();
            }

            Double rcvRate = 0.0;

            if (st != null ) {
                LocalDateTime et = LocalDateTime.now();

                Duration delta = Duration.between(st, et);

                Double elapsedSeconds = (double) delta.getSeconds() + delta.getNano() / 1000000000.0;

                rcvRate = (double) cnt / elapsedSeconds;                
            }                

            System.out.println(cnt + "," + rcvRate);
                
                
            this.socket.close();
            
        } catch (Exception e) {
            e.printStackTrace();
        }        
                
    }
    
}
