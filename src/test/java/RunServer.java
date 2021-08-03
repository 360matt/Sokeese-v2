import fr.i360matt.sokeese.server.SokeeseServer;

import java.io.IOException;

public class RunServer {

    public static void main (String[] args) {

        SokeeseServer server = new SokeeseServer(4000);

        new Thread(() -> {
            try {
                server.listen();
                System.out.println("server closed");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

    }

}
