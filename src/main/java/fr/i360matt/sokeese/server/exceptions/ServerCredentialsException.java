package fr.i360matt.sokeese.server.exceptions;

import java.net.SocketAddress;

public class ServerCredentialsException extends Exception {

    private final SocketAddress address;
    private final int statusCode;
    private final String customCode;

    private final String username;
    private final String password;

    public ServerCredentialsException (final SocketAddress address, final int statusCode, final String customCode, final String username, final String password) {
        this.address = address;
        this.statusCode = statusCode;
        this.customCode = customCode;
        this.username = username;
        this.password = password;
    }

    public SocketAddress getAddress () {
        return this.address;
    }

    public int getStatusCode () {
        return this.statusCode;
    }

    public String getCustomCode () {
        return this.customCode;
    }

    public String getUsername () {
        return this.username;
    }

    public String getPassword () {
        return this.password;
    }

    @Override
    public String toString () {
        return "ServerCredentialsException{" +
                "address=" + address +
                ", statusCode=" + statusCode +
                ", customCode='" + customCode + '\'' +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                '}';
    }
}
