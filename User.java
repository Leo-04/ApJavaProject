package ApJavaProject;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;

/*
 * A basic record to hold the data for a User
 */
public record User(String ip, String id, int port, Socket socket, PrintWriter out, BufferedReader in) { }
