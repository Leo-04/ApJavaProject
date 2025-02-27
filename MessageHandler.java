package ApJavaProject;

import java.io.IOException;
import java.util.Arrays;

/*
* The MessageHandler contains all the functions for the different communications between the sever and client
* It also handles decoding the raw message bytes
 */

public interface MessageHandler {
    // Message constants
    public static final char JOINED = 'J';
    public static final char QUIT = 'Q';
    public static final char COORDINATOR = 'C';
    public static final char MESSAGE = 'M';
    public static final char PRIVATE_MESSAGE = 'P';
    public static final char DATA = 'D';
    public static final char PING_PONG = 'G';

    /*
    * Decodes message from its first byte (command byte)
     */
    default String[] decodeMessageArguments(char command, String msg){
        if (command == JOINED || command == COORDINATOR || command == QUIT || command == PING_PONG) {
            return new String[]{msg};
        }
        else if (command == MESSAGE || command == PRIVATE_MESSAGE){
            //Check for length separator
            if (msg.indexOf(' ') == -1){
                return new String[]{"", "Unknown message received"};
            }
            //Get length
            String idLengthString = msg.substring(0, msg.indexOf(' '));
            int idLength;
            try {
                idLength = Integer.parseInt(idLengthString);
            } catch (NumberFormatException e) {
                return new String[]{"", "Message ID length is not a number"};
            }

            //Get ID and message
            int start = msg.indexOf(' ') + 1;
            String id = msg.substring(start, start + idLength);
            String message =  msg.substring(start + idLength + 1);

            return new String[]{id, message};
        } else if (command==DATA){
            //Check if it's a request or data
            if (msg.isEmpty()){
                return new String[]{};
            }

            //Return data
            return msg.substring(1).split("\\u0000");
        }

        else {
            return null;
        }
    }

    /*
     * Handles messages received
     */
    default void handelReceivedMessage(char command, User user, String[] args) {
        switch (command) {
            case JOINED -> handleMsg_Join(user, args);
            case QUIT -> handleMsg_Quit(user, args[0]);
            case COORDINATOR -> handleMsg_NewCoordinator(user, args[0]);
            case MESSAGE -> handleMsg_Message(user, args[0], args[1]);
            case PRIVATE_MESSAGE -> handleMsg_PrivateMessage(user, args[0], args[1]);
            case PING_PONG -> handleMsg_PingPong(user);
            case DATA -> {
                //Loop though each entry
                User[] users = new User[args.length];
                for (int i = 0; i < args.length; i++){

                    String ip, portString, id;
                    int port;

                    //Get indexes
                    int port_split = args[i].indexOf(':');
                    int id_split = args[i].indexOf(' ');

                    //If the entry is malformed
                    if (port_split == -1||id_split == -1){
                        ip = "Unknown";
                        portString = "-1";
                        id = "";
                    }
                    // Read IP, port and ID
                    else {
                        ip = args[i].substring(0, port_split);
                        portString = args[i].substring(port_split + 1, id_split);
                        id = args[i].substring(id_split + 1);
                    }

                    // Parse port to int
                    try{
                        port = Integer.parseInt(portString);
                    } catch (NumberFormatException e){
                        port = -1;
                    }

                    //Add user entry
                    users[i] = new User(ip, id, port, null, null, null);
                }

                //handle it
                handleMsg_Data(user, users);
            }
            default -> handleMsg(command, user, args);
        }
    }

    default void handelUserIO(User user) throws IOException {
        if (!user.in().ready()){
            return;
        }
        //Read msg
        String msg = receive(user);
        char command;
        String[] args;

        if (msg.isEmpty()){
            args = null;
            command = 0;
        }
        else {
            command = msg.charAt(0);
            args = decodeMessageArguments(command, msg.substring(1));
        }

        // Handle data
        handelReceivedMessage(command, user, args);
    }

    void handleMsg_Quit(User user, String id);
    void handleMsg_Join(User user, String[] args);
    void handleMsg_NewCoordinator(User user, String id);
    void handleMsg_Message(User user, String id, String message);
    void handleMsg_PrivateMessage(User user, String id, String message);
    void handleMsg_Data(User user, User[] users);
    void handleMsg_PingPong(User user);
    void handleMsg(char command, User user, String[] args);

    void sendMsg_Join(User user, String did_join);
    void sendMsg_Quit(User user, String id);
    void sendMsg_NewCoordinator(User user);
    void sendMsg_Message(User user, String id, String message);
    void sendMsg_PrivateMessage(User user, String id, String message);
    void sendMsg_Data(User user);
    void sendMsg_PingPong(User user);
    void sendMsg(char msg, User user, String[] args);

    void send(User user, String msg);
    String receive(User user) throws IOException;
}
