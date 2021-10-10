package cs451;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.DatagramPacket;

public class BoradcastReceiver {
    public void receive(Host host, int nMessages){
        try {
            DatagramSocket udpSocket = new DatagramSocket(host.getPort());
            for (int i = 1; i <= nMessages; i++) {
                byte[] buf = new byte[1024];
                DatagramPacket dp = new DatagramPacket(buf, 1024);
                udpSocket.receive(dp);
                String str = new String(dp.getData(), 0, dp.getLength());
                System.out.println(str);
            }
            udpSocket.close();
        }catch (SocketException socketException){
            socketException.printStackTrace();
        }catch (IOException ioException){
            ioException.printStackTrace();
        }

    }
}
