package cs451;

import javax.sound.midi.Receiver;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.HashMap;

public class Main {

    private static void handleSignal() {
        //immediately stop network packet processing
        System.out.println("Immediately stopping network packet processing.");

        //write/flush output file if necessary
        System.out.println("Writing output.");
    }

    private static void initSignalHandlers() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                handleSignal();
            }
        });
    }

    public static void main(String[] args) throws InterruptedException {
        Parser parser = new Parser(args);
        parser.parse();

        initSignalHandlers();

        // example
        long pid = ProcessHandle.current().pid();
        System.out.println("My PID: " + pid + "\n");
        System.out.println("From a new terminal type `kill -SIGINT " + pid + "` or `kill -SIGTERM " + pid + "` to stop processing packets\n");

        System.out.println("My ID: " + parser.myId() + "\n");
        System.out.println("List of resolved hosts is:");
        System.out.println("==========================");
        for (Host host: parser.hosts()) {
            System.out.println(host.getId());
            System.out.println("Human-readable IP: " + host.getIp());
            System.out.println("Human-readable Port: " + host.getPort());
            System.out.println();
        }
        System.out.println();

        System.out.println("Path to output:");
        System.out.println("===============");
        System.out.println(parser.output() + "\n");

        System.out.println("Path to config:");
        System.out.println("===============");
        System.out.println(parser.config() + "\n");

        System.out.println("Doing some initialization\n");

        Target target = new Target();
        target.populate(parser.config());
        target.display();

        HashMap<Integer, Host> hostHashMap = new HashMap<>();

        for (Host host: parser.hosts())
            hostHashMap.put(host.getId(), host);


        System.out.println("Broadcasting and delivering messages...\n");

        if (parser.myId() != target.getId()){
            broadcast(hostHashMap.get(parser.myId()), hostHashMap.get(target.getId()), target.getnMessages());
        }else{
            BoradcastReceiver receiver = new BoradcastReceiver();
            receiver.receive(hostHashMap.get(parser.myId()));
        }

        // After a process finishes broadcasting,
        // it waits forever for the delivery of messages.
        while (true) {
            // Sleep for 1 hour
            Thread.sleep(60 * 60 * 1000);
        }
    }

    public static void broadcast(Host sourceHost, Host targetHost, int nMessages){
        Broadcaster bc = new Broadcaster();
        bc.send(sourceHost, targetHost);
    }
}
