package ApJavaProject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

//https://docs.oracle.com/javase/tutorial/networking/sockets/index.html

public class Server extends Thread{
    protected record UserData(User user, Socket socket, PrintWriter out, BufferedReader in){}

    protected static final String IP = "hostname";
    protected static final int PORT = 9321;


    protected List<UserData> users;
    protected ServerSocket socket;

    public boolean quit = false;

    /*
    * Creates a server instance and runs it
    */
    public static void main(String[] args) {
        Server server = new Server();

        server.start();
        try{
            server.loop();
        } catch (IOException e){
            throw new RuntimeException(e);
        }
    }

    /*
    * Initializes a server
    */
    public Server(){
        users = new ArrayList<>();

        try {
            socket = new ServerSocket(PORT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /*
    * Runs the sever code to read incoming messages from client ports
     */
    @Override public void run(){
        while (!quit) {
            for(UserData data: users){
                try {
                    //Check for data
                    if (!data.in.ready()){
                        continue;
                    }

                    //Read msg
                    String msg = data.in.readLine();

                    // Handle data
                    handelUserMessage(data, msg);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /*
    * Handles a users message sent to the server
     */
    protected void handelUserMessage(UserData data, String msg) {
        // Quit message
        if (msg.equals("QUIT")){
            users.remove(data);
        }
    }

    /*
    * Main loop to accept multiple clients
     */
    public void loop() throws IOException {
        while (!quit){
            // Waiting for client
            Socket clientSocket = socket.accept();
            //Create streams
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            // Get user ID
            String id = waitUserId(in);
            if (id == null){
                out.println("Invalid User ID");
                continue;
            }

            //Create user data
            UserData data = new UserData(
                new User(clientSocket.getInetAddress().getHostAddress(), id, clientSocket.getPort(), false),
                clientSocket, out, in
            );

            // Add user
            addUserData(data);
        }

        socket.close();
    }

    /*
    * Add a new user to the server
     */
    protected void addUserData(UserData data){
        users.add(data);

        data.out.println("Hello");
    }

    /*
    * Waits for a user ID to be sent via sockets
    *
    * Returns `null` when data is invalid
     */
    protected String waitUserId(BufferedReader in) throws IOException {
        String line = in.readLine();
        if (!line.startsWith("ID:")) {
            return null;
        }

        String id = line.substring(3);
        if (id.equals("")) {
            return null;
        }

        return id;
    }
}
