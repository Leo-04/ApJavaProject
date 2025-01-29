package ApJavaProject;

import java.io.IOException;

public class Main extends Thread implements Cli {
    protected Client client = null;

    public Main()  {
        try {
            client = new Client("0.0.0.0", 4321, "Leo");
        } catch (IOException e) {
            System.out.println("Cannot connect to server");
            return;
        }
    }
    /*
     * Runs the client code to read incoming messages from server
     */
    @Override public void run() {
        client.loopIncomingMessages();
    }
    @Override
    public void start(){
        if (client == null){
            return;
        }

        super.start();

        while (!client.quit){
            String inp = input(">>>\n");
            if (inp.equals("q")){
                client.sendMsg_Quit(client.self);
            }
            else
                client.send(client.self, inp);
        }

        client.close();
    }

    public static void main(String[] args) {
        Main main = new Main();
        main.start();
    }
}