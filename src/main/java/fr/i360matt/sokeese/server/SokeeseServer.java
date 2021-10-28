package fr.i360matt.sokeese.server;

import fr.i360matt.sokeese.common.redistribute.Packet;
import fr.i360matt.sokeese.common.redistribute.SendPacket;
import fr.i360matt.sokeese.server.events.ListenerManager;

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
    private final ListenerManager listenerManager = new ListenerManager(this);
    private final ClientsManager clientsManager = new ClientsManager();

    private final Router router = new Router(this);
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

                executorService.execute(() -> {
                    try {
                        new LoggedClient(this, socket);
                    } catch (Exception e) {
                        e.printStackTrace();
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


    public ListenerManager getListenerManager () {
        return this.listenerManager;
    }

    protected CatcherServer getCatcherServer () {
        return this.catcherServer;
    }

    protected Router getRouter () {
        return this.router;
    }

    public ClientsManager getClientsManager () {
        return this.clientsManager;
    }

    public ServerSocket getSocketServer () {
        return this._server;
    }

    public ScheduledExecutorService getExecutorService () {
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


    public <A> void on (final Class<A> clazz, final BiConsumer<A, CatcherServer.RequestData> biConsumer) {
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
        final Packet packet = new Packet(object, "", SendPacket.random.nextLong());

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
