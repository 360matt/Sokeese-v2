import fr.i360matt.sokeese.client.SokeeseClient;
import fr.i360matt.sokeese.common.SokeeseException;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

public class RunBeta {

    public static void main (String[] args) throws InterruptedException {
        SokeeseClient client_beta = new SokeeseClient();

        new Thread(() -> {
            try {
                client_beta.connect("127.0.0.1", 4000, "beta", "escargot");
                System.out.println("beta closed");
            } catch (IOException | RuntimeException | ClassNotFoundException | SokeeseException e) {
                e.printStackTrace();
            }
        }).start();


        Thread.sleep(200);
        // wait the client connect to server

        TestObj obj = new TestObj();
        client_beta.send("alpha", obj);
        // init preload




        for (int k = 0; k < 500; k++) {
            final AtomicLong chope = new AtomicLong();
            final long start = System.currentTimeMillis();
            for (int i = 0; i < 10_000; i++) {
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
            Thread.sleep(1);
        }


    }

}
