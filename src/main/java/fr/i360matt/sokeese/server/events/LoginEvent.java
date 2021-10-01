package fr.i360matt.sokeese.server.events;

import java.net.Socket;

public abstract class LoginEvent extends EventAbstract {

    private Socket socket;
    private String clientName;
    private String password;

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
    public void callEvent (final ServerEventManager server) {
        server.execEvent(server.LOGIN, this);
    }
}
