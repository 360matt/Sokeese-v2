package fr.i360matt.sokeese.server;

import fr.i360matt.sokeese.server.events.EventAbstract;
import fr.i360matt.sokeese.common.StatusCode;
import fr.i360matt.sokeese.common.redistribute.Packet;
import fr.i360matt.sokeese.common.redistribute.SendPacket;
import fr.i360matt.sokeese.server.events.LoginEvent;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class LoggedClient implements Closeable {

    private final Socket socket;
    private final SokeeseServer server;

    private boolean isEnabled;

    private final ObjectInputStream receiver;
    private final ObjectOutputStream sender;

    private String clientName;

    public LoggedClient (final SokeeseServer server, final Socket _socket) throws Exception {
        try (final Socket socket = _socket) {
            this.socket = socket;
            this.server = server;

            socket.setTcpNoDelay(true);
            try (final ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(this.socket.getOutputStream()))) {
                this.sender = oos;
                try (final ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(this.socket.getInputStream()))) {
                    this.receiver = ois;
                    if (this.doWaitLogin()) {
                        try {
                            this.isEnabled = true;
                            while (this.isEnabled && this.server.getSocketServer() != null) {
                                this.server.getCatcherServer().incomingRequest(this.receiver.readUnshared(), this);
                            }
                        } catch (final EOFException e) {
                            throw e;
                        } catch (final Exception ignored) {
                        }
                    }
                } finally {
                    this.server.getClientsManager().remove(this.clientName);
                }
            }
        }
    }



    private void doSendStatus (final EventAbstract event) throws Exception {
        final StatusCode statusCode = event.getStatusCode();
        sender.writeInt(statusCode.getCode());
        if (statusCode.getCode() == StatusCode.OTHER.getCode())
            sender.writeUTF(event.getStatusCustom());
        sender.flush();

    }

    private boolean doWaitLogin () throws IOException {
        final CompletableFuture<Boolean> isgood = new CompletableFuture<>();

        LoggedClient loggedClient = this;

        final LoginEvent loginEvent = new LoginEvent() {
            @Override
            public void callback () {
                try {
                    doSendStatus(this);

                    if (this.getStatusCode().getCode() < StatusCode.OK.getCode()) {
                        final ServerException serverException = new ServerException();
                        serverException.setSocket(getSocket());
                        serverException.setAddress(getSocket().getRemoteSocketAddress());
                        serverException.setUsername(getClientName());
                        serverException.setPassword(getPassword());

                        if (this.getStatusCode() == StatusCode.CREDENTIALS) {
                            serverException.setType(StatusCode.CREDENTIALS);
                            throw serverException;
                        } else if (this.getStatusCode() == StatusCode.ALREADY_LOGGED) {
                            serverException.setType(StatusCode.ALREADY_LOGGED);
                            throw serverException;
                        } else {
                            serverException.setType(StatusCode.OTHER);
                            serverException.setCustomType(this.getStatusCustom());
                            throw serverException;
                        }
                    }

                    loggedClient.clientName = this.getClientName();
                    loggedClient.getServer().getClientsManager().add(clientName, loggedClient);

                    isgood.complete(true);
                } catch (final Exception ex) {
                    callException(ex);
                    isgood.complete(false);
                    loggedClient.close(); // close the server
                }
            }
        };

        loginEvent.setSocket(this.socket);
        loginEvent.setClientName(this.receiver.readUTF());
        loginEvent.setPassword(this.receiver.readUTF());

        if (loginEvent.getClientName() == null)
            loginEvent.setStatus(StatusCode.CREDENTIALS);
        else if (loginEvent.getClientName().equals(""))
            loginEvent.setStatus(StatusCode.CREDENTIALS);
        else if (loginEvent.getClientName().equals("*"))
            loginEvent.setStatus(StatusCode.CREDENTIALS);
        else if (loginEvent.getClientName().equals("**"))
            loginEvent.setStatus(StatusCode.CREDENTIALS);
        else if (this.server.getClientsManager().exist(loginEvent.getClientName())) {
            loginEvent.setStatus(StatusCode.ALREADY_LOGGED);
        }

        loginEvent.callEvent(getServer().getEvents());

        return isgood.join();
    }


    @Override
    public void close () {
        try {
            this.isEnabled = false;
            this.socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // ___________________________________________  API USE _________________________________________


    public String getClientName () {
        return this.clientName;
    }

    public synchronized void sendOrThrow (final Object object) throws IOException {
        this.sender.writeUnshared(object);
        this.sender.flush();
    }

    public void send (final Object object) {
        try {
            this.sendOrThrow(object);
        } catch (final IOException ignored) { }
    }

    public synchronized void sendOrThrow (final Object... objectArray) throws IOException {
        for (final Object obj : objectArray)
            this.sender.writeUnshared(obj);
        this.sender.flush();
    }

    public void send (final Object... objectArray) {
        try {
            this.sendOrThrow(objectArray);
        } catch (final IOException ignored) { }
    }


    public synchronized void sendOrThrow (final Object object, final Consumer<CatcherServer.ReplyBuilder> consumer) throws IOException {
        final Packet packet = new Packet(object, "", SendPacket.random.nextLong());

        final CatcherServer.ReplyBuilder replyBuilder = this.server.getCatcherServer().getReplyBuilder(
                packet.getIdRequest(),
                this.getClientName()
        );
        consumer.accept(replyBuilder);

        this.sender.writeUnshared(packet);
        this.sender.flush();
    }

    public void send (final Object object, final Consumer<CatcherServer.ReplyBuilder> consumer) {
        try {
            this.sendOrThrow(object, consumer);
        } catch (final IOException ignored) { }
    }

    // ________________________________ //


    public Socket getSocket () {
        return this.socket;
    }

    public SokeeseServer getServer () {
        return this.server;
    }


}
