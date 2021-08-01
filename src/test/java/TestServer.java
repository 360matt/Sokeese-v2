import fr.i360matt.sokeese.client.SokeeseClient;
import fr.i360matt.sokeese.client.exceptions.ClientAlreadyLoggedException;
import fr.i360matt.sokeese.client.exceptions.ClientCodeSentException;
import fr.i360matt.sokeese.client.exceptions.ClientCredentialsException;
import fr.i360matt.sokeese.server.SokeeseServer;

import java.io.IOException;

public class TestServer {



    public static void main (String[] args) throws InterruptedException, IOException {

        System.out.println("Waiting you ready ...");
        System.in.read();

        SokeeseServer server = new SokeeseServer(4000);

        new Thread(() -> {
            try {
                server.listen();
                System.out.println("server closed");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        Thread.sleep(1000);


        SokeeseClient client_alpha = new SokeeseClient();
        SokeeseClient client_beta = new SokeeseClient();

        new Thread(() -> {
            try {
                client_alpha.connect("127.0.0.1", 4000, "alpha", "escargot");
                System.out.println("Alpha closed");
            } catch (IOException | RuntimeException | ClientCodeSentException | ClientCredentialsException | ClientAlreadyLoggedException | ClassNotFoundException  e) {
                e.printStackTrace();
            }
        }).start();

        new Thread(() -> {
            try {
                client_beta.connect("127.0.0.1", 4000, "beta", "escargot");
                System.out.println("beta closed");
            } catch (IOException | RuntimeException | ClientCodeSentException | ClientCredentialsException | ClientAlreadyLoggedException | ClassNotFoundException  e) {
                e.printStackTrace();
            }
        }).start();

        Thread.sleep(100);

        client_alpha.on(TestObj.class, (obj, event) -> {
           // System.out.println(event.getSenderName());
            event.reply(0);
        });

        Thread.sleep(100);

        TestObj obj = new TestObj();


        for (int i = 0; i < 10_000; i++) {
            client_beta.send("alpha", obj, replyBuilder -> {
                replyBuilder.on(String.class, (content, name) -> {
                    System.out.println("réponse en texte: " + content);
                });
                replyBuilder.on(Integer.class, 600, (content, name) -> {
                    System.out.println("réponse en int: " + content);
                });
                replyBuilder.nothing((name) -> {
                    System.out.println("rien eu de: " + name);
                });
            });
        }


        Thread.sleep(2_000);

        server.close();
        client_alpha.close();
        client_beta.close();

        System.out.println("Closed");

        Thread.sleep(10000000);



    }

}
