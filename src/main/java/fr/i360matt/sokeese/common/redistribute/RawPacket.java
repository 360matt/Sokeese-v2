package fr.i360matt.sokeese.common.redistribute;

import java.io.Serializable;
import java.util.Random;

public class RawPacket implements Serializable {

    private static final Random random = new Random();


    private static final long serialVersionUID = 8116309025896557669L;

    private final Object recipient;
    private final Object obj;
    private final long idRequest;


    public RawPacket (final String recipient, final Object obj) {
        this.recipient = recipient;
        this.obj = obj;
        this.idRequest = random.nextLong();
    }

    public RawPacket (final String[] recipient, final Object obj) {
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
