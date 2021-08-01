package fr.i360matt.sokeese.common.redistribute;

import java.io.Serializable;

public class RawPacket implements Serializable {

    private static long count = 0;

    private final Object recipient;
    private final Object obj;
    private final long id;


    public RawPacket (final String recipient, final Object obj) {
        this.recipient = recipient;
        this.obj = obj;
        this.id = count++;
    }

    public RawPacket (final String[] recipient, final Object obj) {
        this.recipient = recipient;
        this.obj = obj;
        this.id = count++;
    }

    public Object getRecipient () {
        return recipient;
    }

    public Object getObj () {
        return obj;
    }

    public long getId () {
        return id;
    }
}
