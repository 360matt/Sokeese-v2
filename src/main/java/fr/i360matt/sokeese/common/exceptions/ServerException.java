package fr.i360matt.sokeese.common.exceptions;

import java.net.Socket;
import java.net.SocketAddress;

public class ServerException extends SokeeseException {

    protected SocketAddress address;
    protected Socket socket;
    protected String clientName;


    public void setAddress (final SocketAddress address) {
        this.address = address;
    }
    public SocketAddress getAddress () {
        return address;
    }

    public void setSocket (final Socket socket) {
        this.socket = socket;
    }
    public Socket getSocket () {
        return socket;
    }

    public void setClientName (final String clientName) {
        this.clientName = clientName;
    }
    public String getClientName () {
        return clientName;
    }
}
