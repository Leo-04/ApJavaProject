package ApJavaProject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DataRequestTest {
    Server server;
    int port = 4321;
    String ip = "0.0.0.0";
    String id = "MyId";

    class ClientDataCheck extends Client{
        String data_msg = null;
        int num_members = 0;
        public ClientDataCheck(String ip, int port, String id) throws IOException {
            super(ip, port, id);
        }

        @Override
        public void handleMsg_Data(User user, User[] users) {
            super.handleMsg_Data(user, users);

            StringBuilder content = new StringBuilder("Users' Data:\n(Coordinator) ");
            for (User u: users){
                content.append(u.ip()).append(":").append(u.port()).append(" ").append(u.id()).append("\n");
            }

            num_members = users.length;
            data_msg = content.toString();
        }
    }

    @Before
    public void createServer(){
        server = new Server(port);
        new Thread(server::start).start();
    }

    @Test
    public void testDataRequest() throws IOException, InterruptedException {
        ClientDataCheck client = new ClientDataCheck(ip, port, id);
        new Thread(() -> {while (!client.quit){client.handleIncomingMessage();}}).start();
        client.sendMsg_Data(client.self);
        Thread.sleep(200);
        System.out.println(client.data_msg);
        assertEquals(
            "Users' Data:\n(Coordinator) "+server.users.get(0).ip()+":"+server.users.get(0).port()
                +" "+server.users.get(0).id()+"\n",

            client.data_msg
        );

        client.close();
    }

    @Test
    public void testActiveMembers() throws IOException, InterruptedException {
        ClientDataCheck client1 = new ClientDataCheck(ip, port, id);
        Client client2 = new Client(ip, port, id+"2");
        new Thread(() -> {while (!client1.quit){client1.handleIncomingMessage();}}).start();
        new Thread(() -> {while (!client2.quit){client2.handleIncomingMessage();}}).start();

        Thread.sleep(Server.PING_LOOP_TIME);
        assertEquals(2, client1.num_members);

        client2.close();

        Thread.sleep(Server.PING_LOOP_TIME);
        assertEquals(1, client1.num_members);

        client1.close();

        Thread.sleep(100);
    }

    @After
    public void closeServer(){
        server.close();
    }
}
