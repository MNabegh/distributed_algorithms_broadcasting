package cs451.perfectLink;

import cs451.Host;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.DatagramPacket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.concurrent.Semaphore;

class StatusBroadcaster implements Runnable {
    private Host sender;
    private Semaphore nHosts;

    public StatusBroadcaster(Host sender, Semaphore nHosts) {
        this.sender = sender;
        this.nHosts = nHosts;
    }

    public void run() {
        Random random = new Random();
        DatagramSocket sendSocket = null;
        try {
            sendSocket = new DatagramSocket();

            while (!Thread.currentThread().isInterrupted()) {
                byte[] upMessage = "U".getBytes(StandardCharsets.UTF_8);
                DatagramPacket dp = new DatagramPacket(upMessage, upMessage.length, sender.getPort());
                sendSocket.send(dp);
                // Backoff
                Thread.sleep(random.nextInt(100));
            }
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            nHosts.release();
        }
    }
}

public class PerfectLinkServer {
    int nSenders;
    Host receiverHost;
    HashMap<Integer, Thread> senderThreadMap;
    int[] receivedCount;

    public PerfectLinkServer(int nSenders, Host receiverHost) {
        this.nSenders = nSenders;
        this.receiverHost = receiverHost;
        this.receivedCount = new int[nSenders];
    }

    public void broadcastStatus(HashMap<Integer, Host> senderMap)
    {
        Host[] senders = senderMap.values().toArray(new Host[0]);
        this.senderThreadMap = new HashMap<>();

        // Thread to broadcast up status to all senders
        Thread broadcastingUP = new Thread(() -> {
            // Semaphore to control number of threads in case stress testing proved that too many threads are being created
            Semaphore nHosts = new Semaphore(senders.length);

            for(int i=1; i<=senders.length; i++){
                try {
                    nHosts.acquire();
                    // Thread to send status to another host
                    Thread senderThread = new Thread(new StatusBroadcaster(senderMap.get(i), nHosts));
                    senderThread.start();
                    senderThreadMap.put(i, senderThread);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        });

        broadcastingUP.start();
    }


    public void receive(int nMessages) {
        try {
            DatagramSocket udpSocket = new DatagramSocket(receiverHost.getPort());
            for (int i = 1; i <= nMessages * nSenders; i++) {
                byte[] buf = new byte[1024];
                DatagramPacket dp = new DatagramPacket(buf, 1024);
                udpSocket.receive(dp);
                String str = new String(dp.getData(), 0, dp.getLength());
                StringTokenizer stringTokenizer = new StringTokenizer(str);
                StringBuilder stringBuilder = new StringBuilder();
            }
            udpSocket.close();
        } catch (SocketException socketException) {
            socketException.printStackTrace();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }

    }
}
