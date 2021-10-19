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
import java.util.StringTokenizer;
import java.util.concurrent.locks.ReentrantLock;

public class PerfectLinkClient {
    private int message;
    private final int nMessages;
    private int messageLogged;
    private ReentrantLock innerLock;
    private int messageToBeLogged;

    public PerfectLinkClient(int nMessages) {
        this.nMessages = nMessages;
        this.message = 1;
        this.innerLock = new ReentrantLock();
        this.messageToBeLogged = 1;
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
                }

                System.out.println("All messages acknowledged");
            } catch (IOException ioException) {
                ioException.printStackTrace();
            } finally {
                if(receiveSocket != null)
                    receiveSocket.close();
            }
        });
        ackThread.start();
    }

    public void send(int sourceHostId, Host targetHost, String outputPath) {
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

                    if(i > messageLogged)
                    {
                        logMessage(i, str+"\n", outputPath);
                        messageLogged++;
                    }
                }
            }
            sendSocket.close();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }

    }

    private void logMessage(int order, String message, String outputPath) {
        System.out.println("Trying to log message: " + message);
        Thread logging = new Thread(() -> {
            try {
                while(true) {
                    if(order !=messageToBeLogged)
                        continue;
                    if(!innerLock.tryLock())
                        continue;
                    FileOutputStream fileOutputStream = new FileOutputStream(outputPath, true);
                    FileChannel channel = fileOutputStream.getChannel();
                    FileLock lock = channel.tryLock();
                    if(lock != null){
                        channel.write(ByteBuffer.wrap(message.getBytes(StandardCharsets.UTF_8)));
                        lock.release();
                        innerLock.unlock();
                        messageToBeLogged++;
                        break;
                    }
                    innerLock.unlock();
                }
            } catch (FileNotFoundException e) {
                System.out.println("File Not Found for message: " + message);
            } catch (IOException ioException) {
                System.out.println("IOExecption for message: " + message);
            }
        });

        logging.start();
    }
}
