package fr.i360matt.sokeese.server.events;

import fr.i360matt.sokeese.common.StatusCode;

import java.util.function.Consumer;


public abstract class EventAbstract {
    protected boolean reLoop = false;
    protected int freeeze = 0;

    protected StatusCode statusCode = StatusCode.OK;
    protected String statusCustom;

    protected Consumer<Exception> exceptConsumer;
    protected Exception exception;

    public void onException (final Consumer<Exception> consumer) {
        this.exceptConsumer = consumer;
    }
    public void callException (final Exception ex) {
        this.exception = ex;
    }

    public abstract void callEvent (final ServerEventManager server);

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




}
