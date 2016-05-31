package Server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by User on 17/5/2016.
 * */
public class MainServer{

    public static void main(String args[]) throws Exception {

        InetAddress addr = InetAddress.getByName("0.0.0.0");

        ServerSocket httpserver = new ServerSocket(4621, 50, addr);

        while(true) {
            try {
                Socket client = httpserver.accept();
                ServerService service = new ServerService(client);
                Thread t = new Thread(service);
                t.start();
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
    }
}
