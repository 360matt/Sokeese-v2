package fr.i360matt.sokeese.server.events;

import fr.i360matt.sokeese.common.StatusCode;

import java.net.Socket;
import java.util.function.Consumer;

public abstract class LoginEvent {

    private Socket socket;
    private String clientName;
    private String password;
    protected boolean reLoop = false;
    protected int freeeze = 0;

    protected StatusCode statusCode = StatusCode.OK;
    protected String statusCustom;

    protected Consumer<Exception> exceptConsumer;
    protected Exception exception;

    public void onException (final Consumer<Exception> consumer) {
        this.exceptConsumer = consumer;
    }
    protected void callException (final Exception ex) {
        this.exception = ex;
    }

    public abstract void callback ();

    public void freeze (final int ms) {
        if (this.freeeze < ms) this.freeeze = ms;
    }
    public int getFreeeze () {
        return this.freeeze;
    }

    public boolean getReLoop () {
        return this.reLoop;
    }
    public void reLoop () {
        this.reLoop = true;
    }
    public void reLoop (final boolean stat) {
        this.reLoop = stat;
    }

    public StatusCode getStatusCode () {
        return statusCode;
    }

    public void setStatus (final StatusCode code) {
        this.statusCode = code;
    }

    public String getStatusCustom () {
        return this.statusCustom;
    }

    public void setStatus (final String customCode) {
        this.statusCode = StatusCode.OTHER;
        this.statusCustom = customCode;
    }


    public void setSocket (final Socket socket) {
        this.socket = socket;
    }
    public void setClientName (final String clientName) {
        this.clientName = clientName;
    }
    public void setPassword (final String password) {
        this.password = password;
    }
    public Socket getSocket () {
        return this.socket;
    }
    public String getClientName () {
        return this.clientName;
    }
    public String getPassword () {
        return this.password;
    }

}
