package cs451.perfectLink;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class Target {
    private int nMessages;
    private int id;

    public boolean populate(String path) {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line = br.readLine();
            String [] split = line.split("\\s+");

            if(split.length != 2)
                return false;

            nMessages = Integer.parseInt(split[0]);
            id  = Integer.parseInt(split[1]);

        } catch (FileNotFoundException ef) {
            ef.printStackTrace();
        } catch (IOException eio) {
            eio.printStackTrace();
        }
        return true;
    }

    public void display(){
        System.out.println(nMessages + " Messages to be sent to process {" + id + "}\n");
    }

    public int getId() {
        return id;
    }

    public int getnMessages() {
        return nMessages;
    }
}
