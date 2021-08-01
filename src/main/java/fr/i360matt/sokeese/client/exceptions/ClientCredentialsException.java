package fr.i360matt.sokeese.client.exceptions;


public class ClientCredentialsException extends Exception {


    private final int statusCode;
    private final String customCode;

    private final String username;
    private final String password;

    public ClientCredentialsException (final int statusCode, final String customCode, final String username, final String password) {
        this.statusCode = statusCode;
        this.customCode = customCode;
        this.username = username;
        this.password = password;
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
        return "ClientCredentialsException{" +
                "statusCode=" + statusCode +
                ", customCode='" + customCode + '\'' +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                '}';
    }
}
