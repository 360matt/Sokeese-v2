package fr.i360matt.sokeese.server.events;

import fr.i360matt.sokeese.common.EventAbstract;
import fr.i360matt.sokeese.server.exceptions.ServerCodeSentException;

import java.io.IOException;
import java.net.Socket;
import java.util.function.Consumer;

public class PreLoginEvent extends EventAbstract {

    private Socket socket;

    private Consumer badCode;
    private Consumer io;
    private Consumer other;

    public void setSocket (final Socket socket) {
        this.socket = socket;
    }
    public Socket getSocket () {
        return this.socket;
    }


    @Override
    public void onException (final Class<? extends Exception> ex, final Consumer<?> consumer) {

        if (ex == ServerCodeSentException.class) {
            this.badCode = consumer;
        } else if (ex == IOException.class) {
            this.io = consumer;
        } else {
            this.other = consumer;
        }

    }

    @Override
    public void callException (final Exception ex) {
        if (ex instanceof ServerCodeSentException) {
            if (this.badCode != null) {
                this.badCode.accept(ex);
            }
        } else if (ex instanceof IOException) {
            if (this.io != null) {
                this.io.accept(ex);
            }
        } else {
            if (this.other != null)
                this.other.accept(ex);
        }
    }
}
