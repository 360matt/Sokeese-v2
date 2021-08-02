package fr.i360matt.sokeese.common.redistribute.reply;

import java.io.Serializable;

public class RawReply implements Serializable {

    private static final long serialVersionUID = 1841792979007601495L;
    private final String recipient;
    private final Object obj;
    private final long id;


    public RawReply (final String recipient, final Object obj, final long id) {
        this.recipient = recipient;
        this.obj = obj;
        this.id = id;
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
