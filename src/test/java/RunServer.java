import fr.i360matt.sokeese.common.StatusCode;
import fr.i360matt.sokeese.server.SokeeseServer;
import fr.i360matt.sokeese.server.events.Listener;
import fr.i360matt.sokeese.server.events.LoginEvent;

import java.io.IOException;

public class RunServer {

    public static void main (String[] args) {

        SokeeseServer server = new SokeeseServer(4000);


        server.getListenerManager().registerListener(new Listener() {
            @Event
            public void onLogin (final LoginEvent event) {
                if (event.getStatusCode().equals(StatusCode.OK)) {

                    // do stuff with:
                    event.getPassword();
                    event.getClientName();

                    // and, now, invalide the auth if you want:
                    event.setStatusCode(StatusCode.CREDENTIALS);
                    event.setStatusCode("custom text");

                }

                System.out.println("try to connect: [id: " + event.getClientName() + "; pwd: " + event.getPassword() + "]");
                // debug only
            }
        });


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
