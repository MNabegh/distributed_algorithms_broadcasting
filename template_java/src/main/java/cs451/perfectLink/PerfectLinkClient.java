package cs451.perfectLink;

import cs451.Host;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.DatagramPacket;
import java.nio.charset.StandardCharsets;
import java.util.StringTokenizer;

public class PerfectLinkClient {
    int message;
    int nMessages;

    public PerfectLinkClient(int nMessages) {
        this.nMessages = nMessages;
        this.message = 1;
    }

    public void waitForUpLink(Host sourceHost) {
        DatagramSocket receiveSocket = null;
        try {
            receiveSocket = new DatagramSocket(sourceHost.getPort());

            System.out.println("Waiting for uplink");

            while (true) {
                byte[] upMessage = new byte[1024];
                DatagramPacket upPacket = new DatagramPacket(upMessage, 1024);
                receiveSocket.receive(upPacket);
                String str = new String(upPacket.getData(), 0, upPacket.getLength());
                System.out.println("received message: "+ str + " " + str.length());
                if (str.charAt(0) == 'U')
                    break;
            }
            System.out.println("Link is ready");
        } catch (IOException ioException) {
            ioException.printStackTrace();
        } finally {
            if(receiveSocket != null)
                receiveSocket.close();
        }
    }

    public void receiveAck(Host sourceHost){
        Thread ackThread = new Thread(() -> {
            DatagramSocket receiveSocket = null;
            try {
                receiveSocket = new DatagramSocket(sourceHost.getPort());

                while (message <= nMessages) {
                    System.out.println("Waiting to receive");
                    byte[] upMessage = new byte[1024];
                    DatagramPacket upPacket = new DatagramPacket(upMessage, 1024);
                    receiveSocket.receive(upPacket);
                    String str = new String(upPacket.getData(), 0, upPacket.getLength());
                    System.out.println("received ack " + str);
                    StringTokenizer stringTokenizer = new StringTokenizer(str);
                    stringTokenizer.nextToken();
                    int messageAcked = Integer.parseInt(stringTokenizer.nextToken());

                    message = messageAcked + 1;
                    System.out.println(str);
                }
                System.out.println("All messages acked");
            } catch (IOException ioException) {
                ioException.printStackTrace();
            } finally {
                if(receiveSocket != null)
                    receiveSocket.close();
            }
        });
        ackThread.start();
    }

    public void send(int sourceHostId, Host targetHost) {
        try {


            DatagramSocket sendSocket = new DatagramSocket();
            InetAddress address = InetAddress.getByName(targetHost.getIp());
            int port = targetHost.getPort();
            int i;
            while (message <= nMessages) {
                for (i = message; i <= message + 3 && i <= nMessages; i++) {
                    String str = sourceHostId + " " + i;
                    byte[] message = str.getBytes(StandardCharsets.UTF_8);
                    DatagramPacket m = new DatagramPacket(message, message.length, address, port);
                    sendSocket.send(m);
                }
            }
            sendSocket.close();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }

    }
}
