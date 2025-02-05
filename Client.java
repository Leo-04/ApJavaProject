package ApJavaProject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/*
* The client that keeps track of messages sent to it
 */
public class Client implements MessageHandler{
    protected List<Message> messages;
    protected Logger log;
    protected User self;

    public boolean quit = false;

    public Client(String ip, int port, String id) throws IOException {
        //Create logger
        log = Logger.getLogger(Server.class.getName());

        // Keep track of messages
        messages = new ArrayList<>();

        //Create socket
        var socket = new Socket(ip, port);
        var out = new PrintWriter(socket.getOutputStream(), true);
        var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        //Create the user object for this client
        self = new User(ip, id, port, false, socket, out, in);

        sendMsg_Join(self, "");
    }

    /*
     * Runs the client code to read incoming messages from server
     */
    public void loopIncomingMessages() {
        while (!quit) {
            try {
                handelUserIO(self);
            } catch (IOException e) {
                log.warning("Error within Server::run(), ignoring to continue with loop");
                log.throwing("Server", "run", e);
            }
        }
    }

    /*
    * Closes the client's socket
     */
    public void close() {
        quit = true;
        try {
            self.out().close();
            self.in().close();
            self.socket().close();
        } catch (IOException e) {
            log.severe("Cannot close Client");
        }
    }

    /*
     * Handles "QUIT" message from server
     */
    @Override
    public void handleMsg_Quit(User user) {
        log.info("Quitting");
        close();
    }

    /*
     * Handles "JOINED" response from the server
     */
    @Override
    public void handleMsg_Join(User user, String[] args) {
        if (args[0].equals("true")){
            log.info("We joined");
        } else {
            log.info("We cannot join, quitting");
            close();
        }
    }

    /*
     * Handles Unknown message from the server
     */
    @Override
    public void handleMsg(char command, User user, String[] args) {
        log.warning("Unknown message received by client " + command + ": " + Arrays.toString(args));
    }

    /*
     * Sends the user's ID then the "JOINED" message
     */
    @Override
    public void sendMsg_Join(User user, String did_join) {
        // Send ID
        send(self, user.id());
        // Send JOINED message
        send(self, JOINED+"");
    }

    /*
     * Sends the QUIT message to the server
     */
    @Override
    public void sendMsg_Quit(User user) {
        quit = true;
        send(self, QUIT+"");
    }


    /*
     * Not used
     */
    @Override
    public void sendMsg(char command, User user, String[] args) {
        log.warning("Sending unknown message " + command + ": " + Arrays.toString(args));
    }

    /*
    * sends data to the server
     */
    @Override
    public void send(User user, String msg) {
        //log.info("Sending: "+msg);
        user.out().println(msg);
    }

    /*
     * receives data from the client
     */
    @Override
    public String receive(User user) throws IOException {
        //Wait for data
        while (!user.in().ready()){}

        String msg = user.in().readLine();

        //log.info("Received: "+msg);

        return msg;
    }
}
