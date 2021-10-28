package fr.i360matt.sokeese.server.events;

import fr.i360matt.sokeese.common.StatusCode;
import fr.i360matt.sokeese.server.LoggedClient;

public class Event {

    private final LoggedClient loggedClient;

    private StatusCode statusCode = StatusCode.OK;
    private String customCode;

    private boolean loop;

    public Event (LoggedClient client) {
        this.loggedClient = client;
    }

    public LoggedClient getLoggedClient () {
        return this.loggedClient;
    }

    public StatusCode getStatusCode () {
        return this.statusCode;
    }
    public void setStatusCode (StatusCode statusCode) {
        this.statusCode = statusCode;
    }

    public String getStatusCustom () {
        return this.customCode;
    }
    public void setStatusCode (final String customCode) {
        this.statusCode = StatusCode.OTHER;
        this.customCode = customCode;
    }

    boolean getLoop () {
        return this.loop;
    }
    void setLoop (final boolean loop) {
        this.loop = loop;
    }

}
