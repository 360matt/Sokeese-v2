package fr.i360matt.sokeese.common.redistribute.reply;

import java.io.Serializable;

public class Reply implements Serializable {

    private final String sender;
    private final Object obj;
    private final long id;


    public Reply (final String sender, final Object obj, final long id) {
        this.sender = sender;
        this.obj = obj;
        this.id = id;
    }


    public String getSender () {
        return sender;
    }

    public Object getObj () {
        return obj;
    }

    public long getId () {
        return id;
    }
}
