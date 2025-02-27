package ApJavaProject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class CoordinatorAssignmentTest {
    Server server;
    int port = 4321;
    String ip = "0.0.0.0";
    String id = "MyId";

    @Before
    public void createServer(){
        server = new Server(port);
        new Thread(server::start).start();
    }

    @Test
    public void testCoordinatorAssignment() throws IOException, InterruptedException {

        Client client1 = new Client(ip, port, id);
        Client client2 = new Client(ip, port, id+"2");
        new Thread(() -> {while (!client1.quit){client1.handleIncomingMessage();}}).start();
        new Thread(() -> {while (!client2.quit){client2.handleIncomingMessage();}}).start();

        Thread.sleep(1000);

        assertEquals(server.users.get(0).id(), client1.self.id());
        assertTrue(client1.isCoordinator);

        client1.close();
        Thread.sleep(1000);

        assertEquals(server.users.get(0).id(), client2.self.id());
        assertTrue(client2.isCoordinator);

        client2.close();
    }

    @After
    public void closeServer(){
        server.close();
    }
}
