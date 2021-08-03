package fr.i360matt.sokeese.server;

import java.io.Closeable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ClientsManager implements Closeable {

    private final Map<String, LoggedClient> users = new HashMap<>();

    public void add (final String username, final LoggedClient instance) {
        this.users.put(username, instance);
    }

    public LoggedClient get (final String username) {
        return this.users.get(username);
    }

    public Set<String> getNames () {
        return this.users.keySet();
    }

    public Collection<LoggedClient> getInstances () {
        return this.users.values();
    }

    public int getCount () {
        return this.users.size();
    }

    public boolean exist (final String username) {
        return this.users.containsKey(username);
    }

    public void remove (final String username) {
        this.users.remove(username);
    }

    public void disconnect (final String username) {
        final LoggedClient loggedClient = this.users.get(username);
        if (loggedClient != null)
            loggedClient.close();
    }

    public void disconnectAll () {
        for (final LoggedClient loggedClient : this.users.values()) {
            if (loggedClient != null)
                loggedClient.close();
        }
    }


    @Override
    public void close () {
        this.users.clear();
    }
}
