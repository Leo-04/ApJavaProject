package ApJavaProject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.logging.Logger;


/*
* The server class that handles clients connecting to it and sending messages between them
* Keeps a current list of users and who is the coordinator
*/

public class Server extends Thread implements MessageHandler{
    // Protected attributes
    protected ArrayList<User> users; // List of all currently joined users
    protected ServerSocket socket; // The socket for the server
    protected Logger log;
    // Tracks if clients are connected
    protected HashMap<String, Boolean> alive;
    protected Timer timer;

    protected static final int PING_LOOP_TIME = 20_000;

    // Public attributes
    public boolean quit = false;


    /*
    * Creates a server instance and runs it
    */
    public static void main(String[] args) {
        // Get port from command args
        int port = Cli.getArgPort(args);

        if (port == -1){
            Cli.outputInvalidPort();
            System.exit(-1);
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

        // Start ping loop
        alive = new HashMap<>();
        timer = new Timer();
        timer.schedule( new TimerTask() {
            public void run() {
                checkClientsAlive();
            }
        }, PING_LOOP_TIME, PING_LOOP_TIME);

        try {
            socket = new ServerSocket(port);
        } catch (IOException e) {
            log.severe("Cannot Create ServerSocket Class");
            System.exit(-1);
        }
        //Add Shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (socket != null){
                close();
            }
        }));

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
                    clientSocket,
                    out,
                    in
                );
                log.info("User ID (is" + ( (id == null)? "": " not") +" null): " + id);

                // Send back join message
                sendMsg_Join(user, (id == null)? "false" : "true");

                if (id == null){
                    // Exit, its invalid ID
                    log.info("Closing user socket");

                    in.close();
                    out.close();
                    clientSocket.close();
                } else {
                    // Add user
                    addUser(user);
                }

            } catch (IOException e){
                log.warning("Error within Server::loopIncomingClients(), ignoring to continue with loop: \n"+e.toString());
            }
        }

        close();
    }

    /*
    * Closes and quits the server
    */
    protected void close(){
        quit = true;

        // kill timer
        timer.cancel();
        timer.purge();

        // Send QUIT to all users
        for (User user: users){
            sendMsg_Quit(user);
        }

        // Close socket
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

        alive.put(user.id(), true);
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
     * Check if clients are still connected
     */
    protected void checkClientsAlive(){
        synchronized(users){
            // Loop users
            for(int i = users.size() - 1; i >= 0; i--){
                User user = users.get(i);
                String id = user.id();

                // Send data to coordinator
                if (i == 0){
                    sendMsg_Data(user);
                }

                if (!alive.containsKey(id) || !alive.get(id)){
                    //Force quit
                    handleMsg_Quit(user);
                    sendMsg_Quit(user);
                } else {
                    // Re-Ping client
                    alive.put(user.id(), false);
                    sendMsg_PingPong(user);
                }
            }
        }
    }

    /*
     * Handles users sending the "QUIT" message
     */
    @Override
    public void handleMsg_Quit(User user) {
        // Check if was coordinator
        boolean wasCoordinator = users.indexOf(user) == 0;

        //Remove user
        users.remove(user);
        try {
            user.out().close();
            user.in().close();
            user.socket().close();
        } catch (IOException e) {
            log.warning("Cannot close user sockets");
        }
        log.info("User Quit: " + user.id());

        // Assign new coordinator
        if (wasCoordinator){
            for (User u: users) {
                sendMsg_NewCoordinator(u);
            }
        }
    }

    /*
     * Handles users sending the "JOINED" message
     */
    @Override
    public void handleMsg_Join(User user, String[] args) {
        if (args[0].isEmpty()){
            log.info("User Joined: " + user.id());

            // Notify them of the current coordinator
            sendMsg_NewCoordinator(user);
        } else {
            send(user, JOINED+"");
        }
    }

    /*
    * Check repose from client, if they are wrong, tell them the correct coordinator
     */
    @Override
    public void handleMsg_NewCoordinator(User user, String id) {
        if (users.size() == 0){
            return;
        }

        if (!users.get(0).id().equals(id)){
            sendMsg_NewCoordinator(user);
        }
    }

    /*
    * When receiving a message from a client, send it to all clients
    */
    @Override
    public void handleMsg_Message(User user, String id, String message) {
        for (User u: users){
            sendMsg_Message(u, user.id(), message);
        }
    }

    /*
     * When receiving a private message from a client, send it to correct client
     */
    @Override
    public void handleMsg_PrivateMessage(User user, String id, String message) {
        // Find user to send to
        User user_to_send_to = null;
        for (User u: users){
            if (u.id().equals(id)){
                user_to_send_to = u;
                break;
            }
        }

        // Check if user exists
        if (user_to_send_to == null){
            log.severe("Unknown user ID: "+id);
            return;
        }

        // Send messages
        sendMsg_PrivateMessage(user_to_send_to, user.id(), message);
        sendMsg_PrivateMessage(user, user.id(), message);
    }

    /*
    * User asked for data, send it to them
    */
    @Override
    public void handleMsg_Data(User user, User[] args) {
        sendMsg_Data(user);
    }

    /*
    * Receives pong from client
    */
    @Override
    public void handleMsg_PingPong(User user) {
        log.info("Pong: "+user.id());
        alive.put(user.id(), true);
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

    /*
    * Sends who the current coordinator is to a user
    */
    @Override
    public void sendMsg_NewCoordinator(User user) {
        log.info("New Coordinator: " + users.get(0).id());
        send(user, COORDINATOR+users.get(0).id());
    }

    /*
    * Send a message to a client
    */
    @Override
    public void sendMsg_Message(User user, String id, String message) {
        log.info("Sending message to: <" + user.id() +"> From <" + id +"> " + message);
        send(user, MESSAGE+""+id.length()+" "+id+" "+message);
    }

    /*
    * send a private message to a client
    */
    @Override
    public void sendMsg_PrivateMessage(User user, String id, String message) {
        log.info("Sending private message to: <" + user.id() +"> From <" + id +"> " + message);
        send(user, PRIVATE_MESSAGE+""+id.length()+" "+id+" "+message);
    }

    /*
    * Send data to a client
    */
    @Override
    public void sendMsg_Data(User user) {
        log.info("Sending data to user: <" +user.id() + ">");

        String message = "";

        for (User u: users){
            message += "\u0000" + u.ip() + ":" + u.port() + " " + u.id();
        }

        send(user, DATA+message);
    }

    /*
    * Sends PING to client
    */
    @Override
    public void sendMsg_PingPong(User user) {
        log.info("Ping: "+user.id());
        send(user, PING_PONG+"");
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
        user.out().println(msg);
    }

    /*
     * receives data from the client
     */
    @Override
    public String receive(User user) throws IOException {
        //Wait for data
        while (!user.in().ready()){}

        //Read data
        String msg = user.in().readLine();

        return msg;
    }
}
