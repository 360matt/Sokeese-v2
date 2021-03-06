package fr.i360matt.sokeese.common.exceptions;

import fr.i360matt.sokeese.common.StatusCode;

public class SokeeseException extends Exception {

    private StatusCode type;
    private String customType;

    private String username;
    private String password;




    public void setType (final StatusCode type) {
        this.type = type;
    }
    public StatusCode getType () {
        return type;
    }

    public void setCustomType (final String customType) {
        this.customType = customType;
    }
    public String getCustomType () {
        return customType;
    }

    public void setUsername (final String username) {
        this.username = username;
    }
    public String getUsername () {
        return username;
    }

    public void setPassword (final String password) {
        this.password = password;
    }
    public String getPassword () {
        return password;
    }


    @Override
    public String toString () {
        return "SokeeseException{" +
                "type=" + type +
                ", customType='" + customType + '\'' +
                '}';
    }
}
