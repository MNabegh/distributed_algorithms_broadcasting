package cs451;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

public class Broadcaster {

    public void send(Host sourceHost, Host targetHost, int nMessages) {
        try {
            DatagramSocket udpSocket = new DatagramSocket(sourceHost.getPort());
            InetAddress address = InetAddress.getByName(targetHost.getIp());
            int port = targetHost.getPort();
            for (int i = 1; i <= nMessages; i++) {
                StringBuilder sb = new StringBuilder();
                sb.append(sourceHost.getId());
                sb.append(' ');
                sb.append(i);
                byte [] message = sb.toString().getBytes(StandardCharsets.UTF_8);
                DatagramPacket m = new DatagramPacket(message, message.length, address, port);
                udpSocket.send(m);
            }
            udpSocket.close();
        }catch (SocketException socketException){
            socketException.printStackTrace();
        }catch (UnknownHostException unknownHostException) {
            unknownHostException.printStackTrace();
        }catch (IOException ioException){
            ioException.printStackTrace();
        }

    }
}
