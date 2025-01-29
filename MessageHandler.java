package ApJavaProject;

import java.io.IOException;

public interface MessageHandler {
    // Message constants
    public static final String JOINED = "JOINED";
    public static final String QUIT = "QUIT";

    default String[] decodeMessage(String msg){
        if (msg.equals(JOINED)){
            return new String[]{JOINED, ""};
        }
        else if (msg.equals(QUIT)){
            return new String[]{QUIT, ""};
        } else if (msg.contains(" ")){
            return msg.split(" ", 2);
        } else {
            return new String[]{msg, ""};
        }
    }

    /*
     * Handles messages received
     */
    default void handelReceivedMessage(String msg, User user) { handelReceivedMessage(msg, user, ""); }
    default void handelReceivedMessage(String msg, User user, String data) {
        switch (msg) {
            case JOINED -> handleMsg_Join(user, data);
            case QUIT -> handleMsg_Quit(user, data);
            default -> handleMsg(msg, user, data);
        }
    }

    void handleMsg_Quit(User user, String data);
    void handleMsg_Join(User user, String did_join);
    void handleMsg(String msg, User user, String data);

    void sendMsg_Join(User user, String did_join);
    void sendMsg_Quit(User user);
    void sendMsg(String msg, User user, String data);

    default void send(User user, String msg, String data){ send(user, msg + " " + data); }
    void send(User user, String msg);
    String receive(User user) throws IOException;
}
