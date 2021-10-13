package cs451.perfectLink;

import cs451.Host;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.DatagramPacket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.concurrent.Semaphore;

class StatusBroadcaster extends Thread {
    private Host sender;
    private Semaphore nHosts;
    private String message;
    private byte[] upMessage;

    public StatusBroadcaster(Host sender, Semaphore nHosts) {
        this.sender = sender;
        this.nHosts = nHosts;
        this.message = "U";
    }

    public void run() {
        Random random = new Random();
        try {
            DatagramSocket sendSocket = new DatagramSocket();
            while (!this.isInterrupted()) {
                upMessage = this.message.getBytes(StandardCharsets.UTF_8);
                InetAddress senderAddress = InetAddress.getByName(sender.getIp());
                DatagramPacket dp = new DatagramPacket(upMessage, upMessage.length, senderAddress, sender.getPort());
                sendSocket.send(dp);
                // Backoff
                Thread.sleep(random.nextInt(100));
            }
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
        } finally {

        }
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

public class PerfectLinkServer {
    int nSenders;
    Host receiverHost;
    HashMap<Integer, StatusBroadcaster> senderThreadMap;
    int[] receivedCount;
    int totalReceived;

    public PerfectLinkServer(int nSenders, Host receiverHost) {
        this.nSenders = nSenders;
        this.receiverHost = receiverHost;
        this.receivedCount = new int[nSenders + 1];
    }

    public void broadcastStatus(HashMap<Integer, Host> senderMap) {
        Host[] senders = senderMap.values().toArray(new Host[0]);
        this.senderThreadMap = new HashMap<>();

        // Thread to broadcast up status to all senders
        Thread broadcastingUP = new Thread(() -> {
            // Semaphore to control number of threads in case stress testing proved that too many threads are being created
            Semaphore nHosts = new Semaphore(senders.length);

            for (Host sender: senderMap.values()) {

                // Thread to send status to another host
                StatusBroadcaster senderThread = new StatusBroadcaster(sender, nHosts);
                senderThread.start();
                senderThreadMap.put(sender.getId(), senderThread);

            }

        });

        broadcastingUP.start();
    }

    public void receive(int nMessages) {
        try {
            DatagramSocket udpSocket = new DatagramSocket(receiverHost.getPort());

            System.out.println("Messages to wait for: " + (nMessages + 1)* nSenders);

            while (totalReceived < (nMessages + 1)* nSenders) {
                byte[] buf = new byte[1024];
                DatagramPacket dp = new DatagramPacket(buf, 1024);
                udpSocket.receive(dp);
                String str = new String(dp.getData(), 0, dp.getLength());
                StringTokenizer stringTokenizer = new StringTokenizer(str);
                StringBuilder stringBuilder = new StringBuilder();

                int senderID = Integer.parseInt(stringTokenizer.nextToken());
                int messageOrder = Integer.parseInt(stringTokenizer.nextToken());

                if (messageOrder != (receivedCount[senderID - 1] + 1))
                    continue;

                if(messageOrder > nMessages) {
                    totalReceived++;
                    System.out.println("total received is " + totalReceived);
                    continue;
                }

                totalReceived++;
                receivedCount[senderID - 1] += 1;
                senderThreadMap.get(senderID).setMessage("A " + messageOrder);

                stringBuilder.append(senderID);
                stringBuilder.append(' ');
                stringBuilder.append(receiverHost.getId());
                stringBuilder.append(' ');
                stringBuilder.append(messageOrder);

                System.out.println(stringBuilder.toString());
                System.out.println("total received is " + totalReceived);
            }

            System.out.println("Loop has ended");

            for (StatusBroadcaster senderThread: senderThreadMap.values())
                senderThread.interrupt();


            udpSocket.close();
            System.out.println("Received all expected messages");
        } catch (SocketException socketException) {
            socketException.printStackTrace();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }

    }
}
