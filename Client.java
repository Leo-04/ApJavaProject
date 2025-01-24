package ApJavaProject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Client {
    protected List<Message> messages;
    protected PrintWriter out;
    protected BufferedReader in;
    protected Socket socket;

    public Client(String ip, int port, String id) throws IOException {
        messages = new ArrayList<>();

        socket = new Socket(ip, port);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        out.println("ID:" + id);

        String msg = in.readLine();
        System.out.println("Server said: " + msg);

        out.println("QUIT");
    }

    /*
    * Closes the client's socket
     */
    public void close() throws IOException {
        out.close();
        in.close();
        socket.close();
    }
}
