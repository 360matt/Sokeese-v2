import fr.i360matt.sokeese.client.SokeeseClient;
import fr.i360matt.sokeese.common.SokeeseException;
import fr.i360matt.sokeese.server.SokeeseServer;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

public class TestDebug {



    public static void main (String[] args) throws InterruptedException {

        // System.out.println("Waiting you ready ...");
        // System.in.read();

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
            } catch (IOException | RuntimeException | ClassNotFoundException | SokeeseException e) {
                e.printStackTrace();
            }
        }).start();

        new Thread(() -> {
            try {
                client_beta.connect("127.0.0.1", 4000, "beta", "escargot");
                System.out.println("beta closed");
            } catch (IOException | RuntimeException | ClassNotFoundException | SokeeseException e) {
                e.printStackTrace();
            }
        }).start();

        Thread.sleep(400);

        client_alpha.on(TestObj.class, (obj, event) -> {
          //  System.out.println(event.getClientName());
            event.reply(0);
        });

        Thread.sleep(100);



        TestObj obj = new TestObj();
        client_beta.send("alpha", obj);
        // init preload




        for (int k = 0; k < 10_000; k++) {
            final AtomicLong chope = new AtomicLong();
            final long start = System.currentTimeMillis();
            for (int i = 0; i < 100; i++) {
                client_beta.send("alpha", obj, (replyBuilder -> {
                    replyBuilder.on(Integer.class, (reply, name) -> {
                        // System.out.println("Réponse integer de: " + chope.incrementAndGet());

                        float newer = chope.incrementAndGet();
                        float diff = System.currentTimeMillis() - start;

                        System.out.println("Vitesse: " + (newer + "/" + diff + ": ") + (newer / diff));
                    });
                    replyBuilder.nothing((name) -> {
                        // System.out.println("Rien eu: " + perdu.incrementAndGet());
                    });
                }));
            }
            System.out.println(System.currentTimeMillis()-start);
            Thread.sleep(200);
        }

        client_alpha.close();
        client_beta.close();
        server.close();

        System.gc();



       // System.out.println(System.currentTimeMillis()-start);

        /* client_alpha.close();
        client_beta.close();
        server.close();  */




        System.out.println("Closed");

        Thread.sleep(10000000);



    }

}
