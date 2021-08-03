package fr.i360matt.sokeese.server;

import fr.i360matt.sokeese.common.EventAbstract;
import fr.i360matt.sokeese.common.StatusCode;
import fr.i360matt.sokeese.common.redistribute.RawPacket;
import fr.i360matt.sokeese.server.events.LoginEvent;
import fr.i360matt.sokeese.server.events.PreLoginEvent;
import fr.i360matt.sokeese.server.exceptions.ServerAlreadyLoggedException;
import fr.i360matt.sokeese.server.exceptions.ServerCodeSentException;
import fr.i360matt.sokeese.server.exceptions.ServerCredentialsException;

import java.io.*;
import java.net.Socket;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class LoggedClient implements Closeable {

    private final Socket socket;
    private final SokeeseServer server;

    private boolean isEnabled;

    private final ObjectInputStream receiver;
    private final ObjectOutputStream sender;

    private String clientName;

    public LoggedClient (final SokeeseServer server, final Socket _socket, final PreLoginEvent preLoginEvent) throws Exception {
        try (final Socket socket = _socket) {
            this.socket = socket;
            this.server = server;

            socket.setTcpNoDelay(true);
            try (final ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(this.socket.getOutputStream()))) {
                this.sender = oos;
                this.doSendStatus(preLoginEvent);
                if (preLoginEvent.getStatusCode() < 0) {
                    throw new ServerCodeSentException(
                            socket.getRemoteSocketAddress(),
                            preLoginEvent.getStatusCode(),
                            preLoginEvent.getStatusCustom()
                    );
                }

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
        final int code = event.getStatusCode();
        sender.writeInt(code);
        if (code == -1)
            sender.writeUTF(event.getStatusCustom());
        sender.flush();

    }

    private boolean doWaitLogin () throws IOException {
        final CompletableFuture<Boolean> isgood = new CompletableFuture<>();

        final LoginEvent loginEvent = new LoginEvent();
        loginEvent.setSocket(this.socket);
        loginEvent.setClientName(this.receiver.readUTF());
        loginEvent.setPassword(this.receiver.readUTF());

        if (Objects.equals(loginEvent.getClientName(), "")) {
            loginEvent.setStatus(StatusCode.Login.INVALID_CREDENTIALS);

        } else if (this.server.getClientsManager().exist(loginEvent.getClientName())) {
            loginEvent.setStatus(StatusCode.Login.ALREADY_LOGGED);
        }

        this.server.getEvents().execEvent(this.server.getEvents().getLogin(), loginEvent, (code, custom) -> {
            try {
                this.doSendStatus(loginEvent);

                if (code == StatusCode.Login.INVALID_CREDENTIALS) {
                    throw new ServerCredentialsException(
                            loginEvent.getSocket().getRemoteSocketAddress(),
                            code,
                            custom,
                            loginEvent.getClientName(),
                            loginEvent.getPassword()
                    );
                }

                if (code == StatusCode.Login.ALREADY_LOGGED) {
                    throw new ServerAlreadyLoggedException(
                            socket.getLocalSocketAddress(),
                            StatusCode.Login.ALREADY_LOGGED,
                            null,
                            loginEvent.getClientName()
                    );
                }


                if (code < 0) {
                    throw new ServerCodeSentException(
                            socket.getRemoteSocketAddress(),
                            loginEvent.getStatusCode(),
                            loginEvent.getStatusCustom()
                    );
                }


                this.clientName = loginEvent.getClientName();

                this.server.getClientsManager().add(this.clientName, this);

                isgood.complete(true);
            } catch (Exception e) {
                loginEvent.callException(e);
                isgood.complete(false);
                this.close(); // close the server
            }
        });
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
        this.sender.writeObject(object);
        this.sender.flush();
    }

    public void send (final Object object) {
        try {
            this.sendOrThrow(object);
        } catch (final IOException ignored) { }
    }


    // ________________________________ //

    public synchronized void sendOrThrow (final String recipient, final Object object) throws IOException {
        RawPacket rawPacket = new RawPacket(recipient, object);
        this.sender.writeObject(rawPacket);
        this.sender.flush();
    }

    public void send (final String recipient, final Object object) {
        try {
            this.sendOrThrow(recipient, object);
        } catch (final IOException ignored) { }
    }

    public synchronized void sendOrThrow (final String[] recipients, final Object object) throws IOException {
        RawPacket rawPacket = new RawPacket(recipients, object);
        this.sender.writeObject(rawPacket);
        this.sender.flush();
    }

    public void send (final String[] recipients, final Object object) {
        try {
            this.sendOrThrow(recipients, object);
        } catch (final IOException ignored) { }
    }

    // ________________________________ //


    public synchronized void sendOrThrow (final Object... objectArray) throws IOException {
        for (final Object obj : objectArray)
            this.sender.writeObject(obj);
        this.sender.flush();
    }

    public void send (final Object... objectArray) {
        try {
            this.sendOrThrow(objectArray);
        } catch (final IOException ignored) { }
    }

    public synchronized void sendOrThrow (final String recipient, final Object... objectArray) throws IOException {
        for (final Object obj : objectArray) {
            final RawPacket rawPacket = new RawPacket(recipient, obj);
            this.sender.writeObject(rawPacket);
        }
        this.sender.flush();
    }

    public void send (final String recipient, final Object... objectArray) {
        try {
            this.sendOrThrow(recipient, objectArray);
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
