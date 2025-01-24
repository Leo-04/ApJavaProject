package ApJavaProject;

import java.io.IOException;

public class Main extends Thread implements Cli {
    protected Client client;

    public static void main(String[] args) {
        Main main = new Main();
    }

    public Main()  {
        try {
            client = new Client("0.0.0.0", 9321, "");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            client.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}