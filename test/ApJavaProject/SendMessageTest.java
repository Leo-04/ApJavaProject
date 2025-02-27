package ApJavaProject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class SendMessageTest {
    Server server;
    int port = 4321;
    String ip = "0.0.0.0";

    @Before
    public void createServer(){
        server = new Server(port);
        new Thread(server::start).start();
    }

    @After
    public void closeServer(){
        server.close();
    }

    @Test
    public void testMessages() throws IOException, InterruptedException {
        String id="ID";
        Client client1 = new Client(ip, port, id);
        Client client2 = new Client(ip, port, id+"2");
        Client client3 = new Client(ip, port, id+"3");
        new Thread(() -> {while (!client1.quit){client1.handleIncomingMessage();}}).start();
        new Thread(() -> {while (!client2.quit){client2.handleIncomingMessage();}}).start();
        new Thread(() -> {while (!client3.quit){client3.handleIncomingMessage();}}).start();

        // Clear other messages
        client1.getNewMessages();
        client2.getNewMessages();
        client3.getNewMessages();

        //Global messages
        Thread.sleep(1000); // Let communications happen
        client1.sendMessage("Hello");
        Thread.sleep(1000); // Let communications happen
        assertEquals("Hello", client1.getNewMessages().getLast().content());
        assertEquals("Hello", client2.getNewMessages().getLast().content());
        assertEquals("Hello", client3.getNewMessages().getLast().content());

        //Private messages
        client1.sendMessage(id+"3", "Hello");
        Thread.sleep(100); // Let communications happen
        assertEquals(0, client2.getNewMessages().size());
        assertEquals("Hello", client3.getNewMessages().get(0).content());

        client1.close();
        client2.close();
        client3.close();
    }
}
