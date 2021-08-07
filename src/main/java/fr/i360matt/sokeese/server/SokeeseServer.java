package fr.i360matt.sokeese.server;

import fr.i360matt.sokeese.common.redistribute.Packet;
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
    private ServerSocket _server;

    private ScheduledExecutorService executorService;
    private final EventsServer events = new EventsServer();
    private final ClientsManager clientsManager = new ClientsManager();
    private final CatcherServer catcherServer = new CatcherServer(this);


    public SokeeseServer (final int port) {
        this.port = port;
    }

    public SokeeseServer (final ServerSocket server) {
        this.port = server.getLocalPort();
        this._server = server;
    }

    public synchronized void listen () throws IOException {
        if (this._server == null)
            this._server = new ServerSocket(this.port);

        try (final ServerSocket server = _server) {
            this.executorService = Executors.newScheduledThreadPool(4);

            while (!server.isClosed()) {
                final Socket socket = server.accept();

                final PreLoginEvent event = new PreLoginEvent();
                event.setSocket(socket);

                this.events.execEvent(this.events.getPreLogin(), event, (code, custom) -> {
                    try {

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
        return this._server;
    }

    protected ScheduledExecutorService getExecutorService () {
        return this.executorService;
    }

    public void close () {
        try {
            if (this._server != null) {
                this._server.close();
                this._server = null;
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

    public void unregisterAll () {
        this.catcherServer.unregisterAll();
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
                loggedClient.send(object);
            }
        }
    }

    public void send (final String[] recipients, final Object... object) {
        for (final String candidateName : recipients) {
            final LoggedClient loggedClient = this.clientsManager.get(candidateName);
            if (loggedClient != null) {
                loggedClient.send(object);
            }
        }
    }

    public void send (final String recipient, final Object object, final Consumer<CatcherServer.ReplyBuilder> consumer) {
        try {
            final LoggedClient loggedClient = this.clientsManager.get(recipient);
            if (loggedClient != null) {
                loggedClient.sendOrThrow(object, consumer);
            }

        } catch (final IOException ignored) { }
    }

    public void send (final String[] recipients, final Object object, final Consumer<CatcherServer.ReplyBuilder> consumer) {
        final Packet packet = new Packet(object, "", RawPacket.random.nextLong());

        final CatcherServer.ReplyBuilder replyBuilder = this.getCatcherServer().getReplyBuilder(
                packet.getIdRequest(),
                recipients
        );
        consumer.accept(replyBuilder);

        for (final String candidateName : recipients) {
            final LoggedClient loggedClient = this.clientsManager.get(candidateName);
            if (loggedClient != null) {
                loggedClient.send(packet);
            }
        }
    }

    public void sendtoAll (final Object object) {
        for (final LoggedClient loggedClient : this.clientsManager.getInstances()) {
            loggedClient.send(object);
        }
    }

    public void sendtoAll (final Object... objectArray) {
        for (final LoggedClient loggedClient : this.clientsManager.getInstances()) {
            loggedClient.send(objectArray);
        }
    }

    public void disconnect (final String user) {
        this.clientsManager.disconnect(user);
    }


    public void disconnectAll () {
        this.clientsManager.disconnectAll();
    }





}
