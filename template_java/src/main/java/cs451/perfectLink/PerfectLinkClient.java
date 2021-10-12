package cs451.perfectLink;

import cs451.Host;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

public class PerfectLinkClient {

    public void waitForUpLink(Host sourceHost) {
        DatagramSocket receiveSocket = null;
        try {
            receiveSocket = new DatagramSocket(sourceHost.getPort());

            while (true) {
                byte[] upMessage = new byte[1024];
                DatagramPacket upPacket = new DatagramPacket(upMessage, 1024);
                receiveSocket.receive(upPacket);
                String str = new String(upPacket.getData(), 0, upPacket.getLength());
                if (str == "U")
                    break;
            }
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    public void send(int sourceHostId, Host targetHost, int nMessages) {
        try {


            DatagramSocket sendSocket = new DatagramSocket();
            InetAddress address = InetAddress.getByName(targetHost.getIp());
            int port = targetHost.getPort();
            for (int i = 1; i <= nMessages; i++) {
                StringBuilder sb = new StringBuilder();
                sb.append(sourceHostId);
                sb.append(' ');
                sb.append(i);
                byte[] message = sb.toString().getBytes(StandardCharsets.UTF_8);
                System.out.println("Message is " + message.length + " bytes");
                DatagramPacket m = new DatagramPacket(message, message.length, address, port);
                sendSocket.send(m);
            }
            sendSocket.close();
        } catch (SocketException socketException) {
            socketException.printStackTrace();
        } catch (UnknownHostException unknownHostException) {
            unknownHostException.printStackTrace();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }

    }
}
