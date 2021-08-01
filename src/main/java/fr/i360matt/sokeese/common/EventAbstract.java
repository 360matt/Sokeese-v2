package fr.i360matt.sokeese.common;

import java.util.function.Consumer;


public abstract class EventAbstract {
    protected boolean reLoop = false;
    protected int freeeze = 0;

    protected int statusCode = 0;
    protected String statusCustom;

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

    public int getStatusCode () {
        return statusCode;
    }

    public void setStatus (final int code) {
        this.statusCode = code;
    }

    public String getStatusCustom () {
        return this.statusCustom;
    }

    public void setStatus (final String customCode) {
        this.statusCode = -1;
        this.statusCustom = customCode;
    }

    public abstract void onException (final Class<? extends Exception> ex, final Consumer<?> consumer);

    public abstract void callException (final Exception ex);


}
