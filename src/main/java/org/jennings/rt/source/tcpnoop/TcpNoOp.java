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
