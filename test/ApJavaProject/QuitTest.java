package ApJavaProject;

import org.junit.Test;

import java.io.IOException;
import static org.junit.Assert.*;

public class QuitTest {
    private class ClientNoQuitTest extends Client{

        public ClientNoQuitTest(String ip, int port, String id) throws IOException {
            super(ip, port, id);
        }

        @Override
        public void sendMsg_Quit(User user, String id) {
            quit = true;
            //Don't send quit message to server for some reason
        }
    }
    private class ClientNotRespondTest extends Client{

        public ClientNotRespondTest(String ip, int port, String id) throws IOException {
            super(ip, port, id);
        }

        @Override
        public void sendMsg_PingPong(User user) {
            //Don't send ping message to server for some reason
        }
    }
    private class ServerTest extends Server{
        public ServerTest(int port) {
            super(port);

        }

        public boolean hasQuit(String id){
            for (int i = users.size() - 1; i >= 0; i--){
                if (users.get(i).id().equals(id))
                    return false;
            }

            return true;
        }
    }

    @Test
    public void testAbnormalClientQuiting() throws IOException, InterruptedException {
        int port = 4321;
        String ip = "0.0.0.0";
        String id = "MyId";

        ServerTest server = new ServerTest(port);
        new Thread(server::start).start();
        ClientNoQuitTest client = new ClientNoQuitTest(ip, port, id);
        client.close();
        new Thread(() -> {while (!client.quit){client.handleIncomingMessage();}}).start();

        Thread.sleep(Server.PING_LOOP_TIME * 3);

        assertTrue(server.hasQuit(id));
        server.close();
    }

    @Test
    public void testClientNotResponding() throws IOException, InterruptedException {
        int port = 4321;
        String ip = "0.0.0.0";
        String id = "MyId";

        ServerTest server = new ServerTest(port);
        new Thread(server::start).start();
        ClientNotRespondTest client = new ClientNotRespondTest(ip, port, id);

        new Thread(() -> {while (!client.quit){client.handleIncomingMessage();}}).start();

        Thread.sleep(Server.PING_LOOP_TIME * 3);

        assertTrue(server.hasQuit(id));
        client.close();
        server.close();
    }
}