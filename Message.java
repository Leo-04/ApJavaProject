package ApJavaProject;

/*
 * A basic record to hold the data for a Message
 */
public record Message(String content, String senderId, long timeStamp, boolean isPrivate) { }
