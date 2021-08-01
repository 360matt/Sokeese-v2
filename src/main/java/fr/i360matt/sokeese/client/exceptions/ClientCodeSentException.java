package fr.i360matt.sokeese.client.exceptions;


public class ClientCodeSentException extends Exception {

    private final int statusCode;
    private final String customCode;
    private final String username;
    private final String password;

    public ClientCodeSentException (final int statusCode, final String customCode, final String username, final String password) {
        this.statusCode = statusCode;
        this.customCode = customCode;
        this.username = username;
        this.password = password;
    }

    public int getStatusCode () {
        return statusCode;
    }

    public String getCustomCode () {
        return customCode;
    }

    public String getUsername () {
        return username;
    }

    public String getPassword () {
        return password;
    }

    @Override
    public String toString () {
        return "ClientCodeSentException{" +
                "statusCode=" + statusCode +
                ", customCode='" + customCode + '\'' +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                '}';
    }
}
