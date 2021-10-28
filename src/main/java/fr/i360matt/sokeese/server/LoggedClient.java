package fr.i360matt.sokeese.server;

import fr.i360matt.sokeese.common.StatusCode;
import fr.i360matt.sokeese.common.redistribute.Packet;
import fr.i360matt.sokeese.common.redistribute.SendPacket;
import fr.i360matt.sokeese.server.events.LoginEvent;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
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
                                this.server.getRouter().incomingRequest(this.receiver.readUnshared(), this);
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



    private void doSendStatus (final LoginEvent event) throws IOException {
        final StatusCode statusCode = event.getStatusCode();
        this.sender.writeInt(statusCode.getCode());
        if (statusCode.getCode() == StatusCode.OTHER.getCode())
            this.sender.writeUTF(event.getStatusCustom());
        this.sender.flush();

    }

    private boolean doWaitLogin () throws IOException {
        final LoginEvent event = new LoginEvent(this);

        this.clientName = this.receiver.readUTF();
        event.setClientName(this.clientName);
        event.setPassword(this.receiver.readUTF());

        this.getServer().getListenerManager().callEvent(event);

        doSendStatus(event);

        if (event.getStatusCode().getCode() != StatusCode.OK.getCode()) {
            this.close(); // close the server
            return false;
        } else {
            this.clientName = this.getClientName();
            this.getServer().getClientsManager().add(this.clientName, this);
            return true;
        }
    }


    @Override
    public void close () {
        if (this.clientName != null) {
            this.getServer().getClientsManager().remove(this.clientName);
        }
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
