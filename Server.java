package ApJavaProject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;


/*
* The server class that handles clients connecting to it and sending messages between them
* Keeps a current list of users and who is the coordinator
*/

public class Server extends Thread implements MessageHandler{
    // Protected attributes
    protected ArrayList<User> users; // List of all currently joined users
    protected ServerSocket socket; // The socket for
    protected Logger log;

    // Public attributes
    public boolean quit = false;

    /*
    * Creates a server instance and runs it
    */
    public static void main(String[] args) {
        // Get port from command args
        if (args.length != 1){
            System.out.println("No Port given");
            return;
        }
        int port;
        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException e){
            System.out.println("Port is not a number");
            return;
        }

        //Create server
        Server server = new Server(port);

        // Start server
        server.start();
    }

    /*
    * Initializes a server
    */
    public Server(int port){
        log = Logger.getLogger(Server.class.getName());
        users = new ArrayList<>();

        try {
            socket = new ServerSocket(port);
        } catch (IOException e) {
            log.severe("Cannot Create ServerSocket Class");
            System.exit(-1);
        }

        log.info("Created Server");
    }

    /*
    * Starts all server loops
     */
    @Override public void start(){
        if (socket == null){
            log.severe("Socket was not created");
            return;
        }

        //Start the thread
        super.start();

        // Loop for clients
        loopIncomingClients();
    }

    /*
    * Runs the sever code to read incoming messages from client ports
     */
    @Override public void run(){
        while (!quit) {
            synchronized(users){
                for(int i = users.size() - 1; i >= 0; i--){
                    try {
                        handelUserIO(users.get(i));
                    } catch (IOException e) {
                        log.warning("Error within Server::run(), ignoring to continue with loop");
                    }
                }
            }
        }
    }

    /*
    * Waits for a client to join and adds them to the server
    */
    protected void loopIncomingClients() {
        // Loop forever
        while (!quit){
            try{
                // Waiting for client
                Socket clientSocket = socket.accept();

                // Create streams
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                log.info("User joined waiting for ID");

                // Get user ID
                String id = waitUserId(in);

                //Create user
                User user = new User(
                    clientSocket.getInetAddress().getHostAddress(),
                    id,
                    clientSocket.getPort(),
                    false,
                    clientSocket,
                    out,
                    in
                );
                log.info("User ID (is" + ( (id == null)? "": " not") +" null): " + id);

                if (id == null){
                    // Exit, its invalid ID
                    log.info("Closing user socket");

                    in.close();
                    out.close();
                    clientSocket.close();
                } else {
                    // Send back join message
                    sendMsg_Join(user, (id == null)? "false" : "true");

                    // Add user
                    addUser(user);
                }

            } catch (IOException e){
                log.warning("Error within Server::loopIncomingClients(), ignoring to continue with loop: \n"+e.toString());
            }
        }

        // Send QUIT to all users
        for (User user: users){
            sendMsg_Quit(user);
        }

        try{
            socket.close();
        } catch (IOException e){
            log.severe("Cannot close socket");
        }
    }

    /*
    * Add a new user to the server
     */
    protected void addUser(User user){
        log.info("Adding user: "+user.id());

        users.add(user);
    }

    /*
    * Waits for a user ID to be sent via sockets
    *
    * Returns `null` when data is invalid
     */
    protected String waitUserId(BufferedReader in) throws IOException {
        String id = in.readLine();
        if (id.isEmpty()) {
            return null;
        }

        // Check if ID already exists
        for (User user: users){
            if (user.id().equals(id)){
                return null;
            }
        }

        return id;
    }

    /*
     * Handles users sending the "QUIT" message
     */
    @Override
    public void handleMsg_Quit(User user) {
        users.remove(user);
        log.info("User Quit: " + user.id());

        try {
            user.out().close();
            user.in().close();
            user.socket().close();
        } catch (IOException e) {
            log.warning("Cannot close user sockets");
        }
    }

    /*
     * Handles users sending the "JOINED" message
     */
    @Override
    public void handleMsg_Join(User user, String[] args) {
        if (args[0].isEmpty()){
            log.info("User Joined: " + user.id());
        } else {
            send(user, JOINED+"");
        }
    }

    /*
     * Handles users sending an unknown message
     */
    @Override
    public void handleMsg(char command, User user, String[] args) {
        log.warning("Unknown message received by client <"+ user.id() +"> " + command + ": " + Arrays.toString(args));
    }

    /*
     * Handles sending users message after they joined
     */
    @Override
    public void sendMsg_Join(User user, String did_join) {
        send(user, JOINED+did_join);
    }

    /*
     * Sends the message to force quit a user
     */
    @Override
    public void sendMsg_Quit(User user) {
        send(user, QUIT+"");
    }

    @Override
    public void sendMsg(char command, User user, String[] args) {
        log.warning("Sending unknown message " + command + ": " + Arrays.toString(args));
    }

    /*
     * sends data to the client
     */
    @Override
    public void send(User user, String msg) {
        //log.info("Sending to <"+user.id()+"> "+msg);

        user.out().println(msg);
    }

    /*
     * receives data from the client
     */
    @Override
    public String receive(User user) throws IOException {
        //Wait for data
        while (!user.in().ready()){}

        //Read datra
        String msg = user.in().readLine();

        //log.info("Received from <"+user.id()+"> "+msg);

        return msg;
    }
}
