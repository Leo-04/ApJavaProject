package ApJavaProject;

import java.io.IOException;

/*
* Main application class
*/

public class Main extends Thread {
    protected Client client;

    public Main(String ip, int port, String id)  {
        //Create client
        try {
            client = new Client(ip, port, id);
        } catch (IOException e) {
            Cli.outputServerError();
            System.exit(-1);
        }

        //Add Shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (client != null){
                client.close();
            }
        }));
    }

    /*
     * Runs the input loop
     */
    @Override public void run() {
        Cli.outputOptionPrompt();

            mainLoop:
            while (!client.quit){
                String option = Cli.inputOption();

                String id, message;
                switch (option) {
                    case "" -> {}
                    case "1" -> {
                        message = Cli.inputMessage();
                        client.sendMessage(message);
                    }
                    case "2" -> {
                        id = Cli.inputID();
                        message = Cli.inputMessage();
                        synchronized (client){client.sendMessage(id, message);}
                    }
                    case "3" -> {
                        synchronized (client){client.requestData();}
                    }
                    case "4" -> {
                        break mainLoop;
                    }
                    default -> Cli.outputInvalidInput(option);
                }
            }

        client.close();
    }
    @Override
    public void start(){
        if (client == null){
            return;
        }

        // Start input thread
        super.start();

        // Loop Client
        while(!client.quit){
            // Handle incoming messages
            client.handleIncomingMessage();

            // Print new message
            for (Message msg: client.getNewMessages()){
                Cli.outputMessage(msg);
            }
        }

        // Stop input thread
        interrupt();
    }

    public static void main(String[] args) {
        // Get port from command line
        int port = Cli.getArgPort(args);
        if (port == -1){
            Cli.outputInvalidPort();
            System.exit(-1);
        }

        // Get ip from command line
        String ip = Cli.getArgIP(args);
        if (ip == null){
            Cli.outputInvalidIP();
            System.exit(-1);
        }

        // Get id from command line
        String id = Cli.getArgID(args);
        if (id == null){
            Cli.outputInvalidID();
            System.exit(-1);
        }

        // Create application
        Main main = new Main(ip, port, id);

        // Start application
        main.start();

        // Exit
        System.exit(0);
    }
}