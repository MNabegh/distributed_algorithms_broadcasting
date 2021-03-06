package cs451.perfectLink;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Target {
    private long nMessages;
    private int id;

    public void populate(String path) {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line = br.readLine();
            String [] split = line.split("\\s+");

            if(split.length != 2)
                return;

            nMessages = Integer.parseInt(split[0]);
            id  = Integer.parseInt(split[1]);

        } catch (IOException eio) {
            eio.printStackTrace();
        }
    }

    public void display(){
        System.out.println(nMessages + " Messages to be sent to process {" + id + "}\n");
    }

    public int getId() {
        return id;
    }

    public long getNMessages() {
        return nMessages;
    }
}
