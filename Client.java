package ApJavaProject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;
import java.util.logging.Logger;

/*
* The client that keeps track of messages sent to it
 */
public class Client implements MessageHandler{
    protected List<Message> messages;
    protected int newMessages = 0;
    protected Logger log;
    protected User self;
    protected boolean isCoordinator = false;

    //Public quit flag
    public boolean quit = false;

    public Client(String ip, int port, String id) throws IOException {
        super();

        //Create logger
        log = Logger.getLogger(Server.class.getName());

        // Remove new-line characters
        id = id.replace("\\n", "\\r").replace("\\r", " ").replace("\\u0000", " ");

        // Keep track of messages
        messages = new ArrayList<>();

        //Create socket
        var socket = new Socket(ip, port);
        var out = new PrintWriter(socket.getOutputStream(), true);
        var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        //Create the user object for this client
        self = new User(ip, id, port, socket, out, in);

        sendMsg_Join(self, "");
    }

    /*
     * Runs the client code to read incoming message from server
     */
    public void handleIncomingMessage() {
        try {
            handelUserIO(self);
        } catch (IOException e) {
            log.warning("Error within Client::run(), ignoring to continue with loop");
        }
    }

    /*
    * Closes the client's socket
     */
    public void close() {
        sendMsg_Quit(self);

        quit = true;
        try {
            self.out().close();
            self.in().close();
            self.socket().close();
        } catch (IOException e) {
            log.severe("Cannot close Client");
        }
    }

    protected void addMessage(String msg, String id, boolean isPrivate){
        newMessages += 1;
        messages.add(new Message(msg, id, System.currentTimeMillis(), isPrivate));
    }

    /*
    * Gets the list of new message since this method was called
     */
    public List<Message> getNewMessages(){
        List<Message> newList = messages.subList(messages.size() - newMessages, messages.size());
        newMessages = 0;
        return newList;
    }

    /*
     * Sends a message
     */
    public void sendMessage(String message){
        sendMsg_Message(self, null, message);
    }

    /*
     * Sends a private message
     */
    public void sendMessage(String id, String message){
        sendMsg_PrivateMessage(self, id, message);
    }

    public void requestData(){
        sendMsg_Data(self);
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
            addMessage("You have joined", "", true);
        } else {
            log.info("We cannot join, quitting");
            addMessage("You have not joined, bad user ID", "", true);
            close();
        }
    }

    /*
    * New coordinator has been chosen
    */
    @Override
    public void handleMsg_NewCoordinator(User user, String id) {
        isCoordinator = user.id().equals(id);
        if (isCoordinator){
            addMessage("You are now the coordinator", "", false);

            // Tell the server we know we are
            sendMsg_NewCoordinator(self);
        } else {
            addMessage("New coordinator: " + id, "", false);

            // Tell the server we know we are not
            sendMsg_NewCoordinator(new User("", id, 0, null, null, null));
        }

    }

    /*
    * Handles a new message
    */
    @Override
    public void handleMsg_Message(User user, String id, String message) {
        addMessage(message, id, false);
    }

    /*
     * Handles a new private message
     */
    @Override
    public void handleMsg_PrivateMessage(User user, String id, String message) {
        addMessage(message, id, true);
    }

    /*
     * Handles getting sent data
     */
    @Override
    public void handleMsg_Data(User user, User[] users) {
        StringBuilder content = new StringBuilder("Users' Data:");
        for (User u: users){
            content.append("\n").append(u.ip()).append(":").append(u.port()).append(" ").append(u.id());
        }

        addMessage(content.toString(), "", true);
    }

    /*
    * Send pong back from ping
    */
    @Override
    public void handleMsg_PingPong(User user) {
        log.info("Pong: "+user.id());
        sendMsg_PingPong(user);
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
    * Sends response back saying who we think the coordinator is
    */
    @Override
    public void sendMsg_NewCoordinator(User user) {
        send(self, COORDINATOR+user.id());
    }

    /*
    * Sends a global message from us
    */
    @Override
    public void sendMsg_Message(User user, String id, String message) {
        send(user, MESSAGE+""+user.id().length()+" "+user.id()+" "+ message);
    }

    /*
     * Sends a global message to someone
     */
    @Override
    public void sendMsg_PrivateMessage(User user, String id, String message) {
        send(user, PRIVATE_MESSAGE+""+id.length()+" "+id+" "+message);
    }

    /*
     * Requests data
     */
    @Override
    public void sendMsg_Data(User user) {
        send(user, DATA+"");
    }

    /*
    * Sends pong back
    */
    @Override
    public void sendMsg_PingPong(User user) {
        send(user, PING_PONG+"");
    }


    /*
     * Not used
     */
    @Override
    public void sendMsg(char command, User user, String[] args) {
        log.warning("Sending unknown message " + command + ": " + Arrays.toString(args));
    }

    /*
    * Sends data to the server
     */
    @Override
    public void send(User user, String msg) {
        user.out().println(msg);
    }

    /*
     * receives data from the server
     */
    @Override
    public String receive(User user) throws IOException {
        //Wait for data
        while (!user.in().ready()){}

        String msg = user.in().readLine();

        return msg;
    }

}
