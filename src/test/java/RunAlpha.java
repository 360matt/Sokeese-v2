import fr.i360matt.sokeese.client.SokeeseClient;
import fr.i360matt.sokeese.client.exceptions.ClientAlreadyLoggedException;
import fr.i360matt.sokeese.client.exceptions.ClientCodeSentException;
import fr.i360matt.sokeese.client.exceptions.ClientCredentialsException;

import java.io.IOException;

public class RunAlpha {

    public static void main (String[] args) throws InterruptedException {
        SokeeseClient client_alpha = new SokeeseClient();

        new Thread(() -> {
            try {
                client_alpha.connect("127.0.0.1", 4000, "alpha", "escargot");
                System.out.println("Alpha closed");
            } catch (IOException | RuntimeException | ClientCodeSentException | ClientCredentialsException | ClientAlreadyLoggedException | ClassNotFoundException  e) {
                e.printStackTrace();
            }
        }).start();

        Thread.sleep(200);
        // wait the client connect to server

        client_alpha.on(TestObj.class, (obj, event) -> {
           // event.reply(0);
        });
    }

}
