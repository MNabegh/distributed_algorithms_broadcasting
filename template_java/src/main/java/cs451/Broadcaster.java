package cs451;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

public class Broadcaster {

    public void send(Host sourceHost, Host targetHost) {
        try {
            DatagramSocket udpSocket = new DatagramSocket(sourceHost.getPort());
            InetAddress address = InetAddress.getByName(targetHost.getIp());
            int port = targetHost.getPort();
            byte [] message = "Hello".getBytes(StandardCharsets.UTF_8);
            DatagramPacket m = new DatagramPacket(message, message.length, address, port);
        }catch (SocketException socketException){
            socketException.printStackTrace();
        }catch (UnknownHostException unknownHostException) {
            unknownHostException.printStackTrace();
        }

    }
}
