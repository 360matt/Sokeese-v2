package fr.i360matt.sokeese.server;

import fr.i360matt.sokeese.common.redistribute.RawPacket;
import fr.i360matt.sokeese.server.events.LoginEvent;
import fr.i360matt.sokeese.server.events.PreLoginEvent;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class SokeeseServer implements Closeable {

    private final int port;
    private ServerSocket server;

    private ScheduledExecutorService executorService;
    private final EventsServer events = new EventsServer();
    private final ClientsManager clientsManager = new ClientsManager();
    private final CatcherServer catcherServer = new CatcherServer(this);


    public SokeeseServer (final int port) {
        this.port = port;
    }

    public SokeeseServer (final ServerSocket server) {
        this.port = server.getLocalPort();
        this.server = server;
    }

    public synchronized void listen () throws IOException {
        if (this.server == null)
            this.server = new ServerSocket(this.port);

        this.executorService = Executors.newScheduledThreadPool(4);

        while (this.server != null && !this.server.isClosed()) {
            final PreLoginEvent event = new PreLoginEvent();
            this.events.execEvent(this.events.getPreLogin(), event, (code, custom) -> {
                try {
                    final Socket socket = this.server.accept();
                    executorService.execute(() -> {
                        try {
                            new LoggedClient(this, socket, event);
                        } catch (Exception e) {
                            event.callException(e);
                        }
                    });
                } catch (Exception e) {
                    event.callException(e);
                    this.close();
                }
            });
        }

        this.clientsManager.close();
        if (!this.executorService.isShutdown()) {
            this.executorService.shutdownNow();
            this.executorService = null;
        }
    }


    protected EventsServer getEvents () {
        return this.events;
    }

    protected CatcherServer getCatcherServer () {
        return this.catcherServer;
    }

    public ClientsManager getClientsManager () {
        return this.clientsManager;
    }

    public ServerSocket getSocketServer () {
        return server;
    }

    protected ScheduledExecutorService getExecutorService () {
        return this.executorService;
    }

    public void close () {
        try {
            if (this.server != null) {
                this.server.close();
                this.server = null;
            }
        } catch (final Exception ignored) { }
    }




    // ___________________________________________  API USE _________________________________________


    public void addLoginEvent (final Consumer<LoginEvent> event) {
        this.events.getLogin().add(event);
    }

    public void addPreLoginEvent (final Consumer<PreLoginEvent> event) {
        this.events.getPreLogin().add(event);
    }

    public <A> void on (final Class<A> clazz, final BiConsumer<A, CatcherServer.OnRequest> biConsumer) {
        this.catcherServer.on(clazz, biConsumer);
    }

    public void unregister (final Class<?> clazz) {
        this.catcherServer.unregister(clazz);
    }

    public void unregisterAll (final Class<?> clazz) {
        this.catcherServer.unregisterAll(clazz);
    }

    public LoggedClient getClient (final String name) {
        return this.clientsManager.get(name);
    }


    // _____


    public void send (final String user, final Object object) {
        final LoggedClient loggedClient = this.clientsManager.get(user);
        if (loggedClient != null) {
            loggedClient.send(object);
        }
    }

    public void send (final String user, final Object... objectArray) {
        final LoggedClient loggedClient = this.clientsManager.get(user);
        if (loggedClient != null) {
            loggedClient.send(objectArray);
        }
    }

    public void send (final String[] recipients, final Object object) {
        for (final String candidateName : recipients) {
            final LoggedClient loggedClient = this.clientsManager.get(candidateName);
            if (loggedClient != null) {
                RawPacket rawPacket = new RawPacket(recipients, object);
                loggedClient.send(rawPacket);
            }
        }
    }

    public void send (final String[] recipients, final Object... object) {
        for (final String candidateName : recipients) {
            final LoggedClient loggedClient = this.clientsManager.get(candidateName);
            if (loggedClient != null) {
                RawPacket rawPacket = new RawPacket(recipients, object);
                loggedClient.send(rawPacket);
            }
        }
    }

    public void sendtoAll (final Object object) {
        for (final LoggedClient loggedClient : this.clientsManager.getInstances()) {
            if (loggedClient != null) {
                loggedClient.send(object);
            }
        }
    }

    public void sendtoAll (final Object... objectArray) {
        for (final LoggedClient loggedClient : this.clientsManager.getInstances()) {
            if (loggedClient != null) {
                loggedClient.send(objectArray);
            }
        }
    }

    public void disconnect (final String user) {
        this.clientsManager.disconnect(user);
    }


    public void disconnectAll () {
        this.clientsManager.disconnectAll();
    }





}
