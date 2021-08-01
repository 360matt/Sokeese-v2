package fr.i360matt.sokeese.client.exceptions;

public class ClientAlreadyLoggedException extends Exception {
    private final String username;
    private final int statusCode;
    private final String customCode;

    public ClientAlreadyLoggedException (final int statusCode, final String customCode, final String username) {
        this.username = username;
        this.statusCode = statusCode;
        this.customCode = customCode;
    }


    public int getStatusCode () {
        return statusCode;
    }

    public String getCustomCode () {
        return customCode;
    }

    public String username () {
        return username;
    }

    @Override
    public String toString () {
        return "ClientAlreadyLoggedException{" +
                "username='" + username + '\'' +
                ", statusCode=" + statusCode +
                ", customCode='" + customCode + '\'' +
                '}';
    }
}
