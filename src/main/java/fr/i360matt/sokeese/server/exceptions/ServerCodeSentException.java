package fr.i360matt.sokeese.server.exceptions;

import java.net.SocketAddress;

public class ServerCodeSentException extends Exception {

    private final SocketAddress address;
    private final int statusCode;
    private final String customCode;

    public ServerCodeSentException (final SocketAddress address, final int statusCode, final String customCode) {
        this.address = address;
        this.statusCode = statusCode;
        this.customCode = customCode;
    }

    public SocketAddress getAddress () {
        return address;
    }

    public int getStatusCode () {
        return statusCode;
    }

    public String getCustomCode () {
        return customCode;
    }

    @Override
    public String toString () {
        return "ServerCodeSentException{" +
                "address=" + address +
                ", statusCode=" + statusCode +
                ", customCode='" + customCode + '\'' +
                '}';
    }
}
