package fr.i360matt.sokeese.server.events;

import fr.i360matt.sokeese.common.EventAbstract;
import fr.i360matt.sokeese.server.exceptions.ServerAlreadyLoggedException;
import fr.i360matt.sokeese.server.exceptions.ServerCodeSentException;
import fr.i360matt.sokeese.server.exceptions.ServerCredentialsException;

import java.io.IOException;
import java.net.Socket;
import java.util.function.Consumer;

public class LoginEvent extends EventAbstract {

    private Socket socket;
    private String clientName;
    private String password;

    private Consumer badCredentials;
    private Consumer badCode;
    private Consumer alreadyLogged;
    private Consumer io;
    private Consumer other;

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

    @Override
    public void onException (final Class<? extends Exception> ex, final Consumer<?> consumer) {

         if (ex == IOException.class) {
            this.io = consumer;
        } else if (ex == ServerCodeSentException.class) {
            this.badCode = consumer;
        } else if (ex == ServerCredentialsException.class) {
            this.badCredentials = consumer;
        } else if (ex == ServerAlreadyLoggedException.class) {
            this.alreadyLogged = consumer;
        } else {
            this.other = consumer;
        }

    }

    @Override
    public void callException (final Exception ex) {

        if (ex instanceof IOException) {
            if (this.io != null) {
                this.io.accept(ex);
            }
        } else if (ex instanceof ServerCodeSentException) {
            if (this.badCode != null) {
                this.badCode.accept(ex);
            }
        } else if (ex instanceof ServerCredentialsException) {
            if (this.badCredentials != null) {
                this.badCredentials.accept(ex);
            }
        } else if (ex instanceof ServerAlreadyLoggedException) {
            if (this.alreadyLogged != null) {
                this.alreadyLogged.accept(ex);
            }
        } else {
            if (this.other != null)
                this.other.accept(ex);
        }

    }

}
