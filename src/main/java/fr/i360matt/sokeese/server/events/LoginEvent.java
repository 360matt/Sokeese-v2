package fr.i360matt.sokeese.server.events;

import fr.i360matt.sokeese.server.LoggedClient;

public class LoginEvent extends Event {
    private String clientName;
    private String password;

    public LoginEvent (final LoggedClient client) {
        super(client);
    }

    public String getClientName () {
        return clientName;
    }
    public void setClientName (final String clientName) {
        this.clientName = clientName;
    }

    public String getPassword () {
        return password;
    }
    public void setPassword (final String password) {
        this.password = password;
    }
}
