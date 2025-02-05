package ApJavaProject;

import java.io.IOException;

/*
* The MessageHandler contains all the functions for the different communications between the sever and client
* It also handles decoding the raw message bytes
 */

public interface MessageHandler {
    // Message constants
    public static final char JOINED = 'J';
    public static final char QUIT = 'Q';

    default String[] decodeMessageArguments(char command, String msg){
        if (command == JOINED){
            return new String[]{msg};
        } else if (command == QUIT) {
            return new String[]{msg};
        } else {
            return null;
        }
    }

    /*
     * Handles messages received
     */
    default void handelReceivedMessage(char command, User user, String[] args) {
        switch (command) {
            case JOINED -> handleMsg_Join(user, args);
            case QUIT -> handleMsg_Quit(user);
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

    void handleMsg_Quit(User user);
    void handleMsg_Join(User user, String[] args);
    void handleMsg(char command, User user, String[] args);

    void sendMsg_Join(User user, String did_join);
    void sendMsg_Quit(User user);
    void sendMsg(char msg, User user, String[] args);

    void send(User user, String msg);
    String receive(User user) throws IOException;
}
