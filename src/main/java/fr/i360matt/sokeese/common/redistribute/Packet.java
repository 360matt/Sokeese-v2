package fr.i360matt.sokeese.common.redistribute;

import java.io.Serializable;

public final class Packet implements Serializable {

    private final Object obj;
    private final String sender;
    private final long idRequest;

    public Packet (final Object obj, final String sender, final long idRequest) {
        this.obj = obj;
        this.sender = sender;
        this.idRequest = idRequest;
    }

    public Object getObj () {
        return obj;
    }

    public String getSender () {
        return sender;
    }

    public long getIdRequest () {
        return idRequest;
    }
}
