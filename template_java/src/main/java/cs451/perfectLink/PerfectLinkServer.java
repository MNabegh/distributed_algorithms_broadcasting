package cs451.perfectLink;

import cs451.Host;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.concurrent.locks.ReentrantLock;

class StatusBroadcaster extends Thread {
    private final Host sender;
    private String message;


    public StatusBroadcaster(Host sender) {
        this.sender = sender;
        this.message = "U";
    }

    public void run() {
        Random random = new Random();
        try {
            DatagramSocket sendSocket = new DatagramSocket();
            while (!this.isInterrupted()) {
                byte[] upMessage = this.message.getBytes(StandardCharsets.UTF_8);
                InetAddress senderAddress = InetAddress.getByName(sender.getIp());
                DatagramPacket dp = new DatagramPacket(upMessage, upMessage.length, senderAddress, sender.getPort());
                sendSocket.send(dp);
                // Backoff
                Thread.sleep(random.nextInt(100));
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

public class PerfectLinkServer {
    private final int nSenders;
    private final Host receiverHost;
    private HashMap<Integer, StatusBroadcaster> senderThreadMap;
    private int[] receivedCount;
    private int totalReceived;
    private ReentrantLock innerLock;
    private volatile int [] messageToBeLogged;
    private static boolean terminated;

    public PerfectLinkServer(int nSenders, Host receiverHost) {
        this.nSenders = nSenders;
        this.receiverHost = receiverHost;
        this.receivedCount = new int[nSenders + 1];
        this.innerLock = new ReentrantLock();
        this.messageToBeLogged = new int[nSenders + 1];
    }

    public void broadcastStatus(HashMap<Integer, Host> senderMap) {
        this.senderThreadMap = new HashMap<>();

        // Thread to broadcast up status to all senders
        Thread broadcastingUP = new Thread(() -> {
            // Semaphore to control number of threads in case stress testing proved that too many threads are being created
//            Semaphore nHosts = new Semaphore(senders.length);

            for (Host sender : senderMap.values()) {

                // Thread to send status to another host
                StatusBroadcaster senderThread = new StatusBroadcaster(sender);
                senderThread.start();
                senderThreadMap.put(sender.getId(), senderThread);

            }

        });

        broadcastingUP.start();
    }

    public void receive(long nMessages, String outputPath) {
        try {
            DatagramSocket udpSocket = new DatagramSocket(receiverHost.getPort());

            System.out.println("Messages to wait for: " + (nMessages  * nSenders));

            while (totalReceived < (nMessages + 1) * nSenders) {
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

                if (messageOrder > nMessages) {
                    totalReceived++;
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
                stringBuilder.append('\n');

                if(terminated){
                    for (StatusBroadcaster sb: senderThreadMap.values())
                        sb.interrupt();

                    break;
                }

                logMessage(senderID, messageOrder, stringBuilder.toString(), outputPath);
            }

            System.out.println("Loop has ended");

            udpSocket.close();
            System.out.println("Received all expected messages");
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }

    }

    private void logMessage(int senderID, int order, String message, String outputPath) {
        System.out.println("Trying to log message: " + message);
        Thread logging = new Thread(() -> {
            try {
                while(true) {
                    if(order != (messageToBeLogged[senderID - 1] + 1))
                        continue;
                    if(!innerLock.tryLock())
                        continue;
                    FileOutputStream fileOutputStream = new FileOutputStream(outputPath, true);
                    FileChannel channel = fileOutputStream.getChannel();
                    FileLock lock = channel.tryLock();
                    if(lock != null){
                        channel.write(ByteBuffer.wrap(message.getBytes(StandardCharsets.UTF_8)));
                        lock.release();
                        messageToBeLogged[senderID - 1]++;
                        innerLock.unlock();
                        break;
                    }
                }
            } catch (FileNotFoundException e) {
                System.out.println("File Not Found for message: " + message);
            } catch (IOException ioException) {
                System.out.println("IOExecption for message: " + message);
            }
        });

        logging.start();
    }

    public static void setTerminated(){
        terminated = true;
    }
}
