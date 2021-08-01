package fr.i360matt.sokeese.server.exceptions;

import java.net.SocketAddress;

public class ServerAlreadyLoggedException extends Exception {
    private final SocketAddress address;
    private final String username;
    private final int statusCode;
    private final String customCode;

    public ServerAlreadyLoggedException (final SocketAddress address, final int statusCode, final String customCode, final String username) {
        this.address = address;
        this.username = username;
        this.statusCode = statusCode;
        this.customCode = customCode;
    }

    public SocketAddress getAddress () {
        return address;
    }

    public String username () {
        return username;
    }

    public int getStatusCode () {
        return statusCode;
    }

    public String getCustomCode () {
        return customCode;
    }

}
