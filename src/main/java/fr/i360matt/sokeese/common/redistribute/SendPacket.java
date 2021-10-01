package fr.i360matt.sokeese.common.redistribute;

import java.io.Serializable;
import java.util.Random;

public final class SendPacket implements Serializable {

    public static final Random random = new Random();


    private final Object recipient;
    private final Object obj;
    private final long idRequest;


    public SendPacket (final String recipient, final Object obj) {
        this.recipient = recipient;
        this.obj = obj;
        this.idRequest = random.nextLong();
    }

    public SendPacket (final String[] recipient, final Object obj) {
        this.recipient = recipient;
        this.obj = obj;
        this.idRequest = random.nextLong();
    }

    public Object getRecipient () {
        return this.recipient;
    }

    public Object getObj () {
        return this.obj;
    }

    public long getId () {
        return this.idRequest;
    }
}
