package fr.i360matt.sokeese.client;

import fr.i360matt.sokeese.common.exceptions.SokeeseException;
import fr.i360matt.sokeese.common.StatusCode;
import fr.i360matt.sokeese.common.redistribute.SendPacket;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class SokeeseClient implements Closeable {

    private Socket _socket;
    private ObjectInputStream receiver;
    private ObjectOutputStream sender;

    private boolean isEnabled;

    private final CatcherClient catcherClient = new CatcherClient(this);

    private ScheduledExecutorService executorService;
    private CompletableFuture<Void> threadSafe;



    public void connect (final String host, final int port, final String username, final String password) throws IOException, ClassNotFoundException, SokeeseException {
        this.connect(new Socket(host, port), username, password);
    }

    public void connect (final Socket _socket, final String username, final String password) throws IOException, RuntimeException, ClassNotFoundException, SokeeseException {
        if (this._socket != null)
            return;

        this.executorService = Executors.newScheduledThreadPool(8);
        this.threadSafe = new CompletableFuture<>();
        try (final Socket socket = _socket) {
            this._socket = socket;
            socket.setTcpNoDelay(true);

            try (final ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()))) {
                this.sender = oos;
                this.doSendCredentials(username, password);
                try (final ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()))) {
                    this.receiver = ois;
                    this.doWaitLoginStatus(username, password);

                    this.threadSafe.complete(null);
                    this.threadSafe = null;

                    this.isEnabled = true;
                    while (this.isEnabled) {
                        if (!socket.isClosed()) {
                            this.catcherClient.incomingRequest(this.receiver.readUnshared());
                            continue;
                        }
                        break;
                    }
                    // the client will shuting down here ...
                }
            }
        } catch (final Exception e) {
            if (e instanceof SocketException || e instanceof EOFException)
                return;
            throw e;
        } finally {
            this.executorService.shutdownNow();
            this.catcherClient.close();
        }
    }

    private void doSendCredentials (final String username, final String password) throws IOException {
        this.sender.writeUTF(username);
        this.sender.writeUTF(password);
        this.sender.flush();
    }

    private void doWaitLoginStatus (final String username, final String password) throws IOException, SokeeseException {
        final int statusLogin = this.receiver.readInt();

        if (statusLogin < StatusCode.OK.getCode()) {
            final SokeeseException sokeeseException = new SokeeseException();
            sokeeseException.setUsername(username);
            sokeeseException.setPassword(password);

            if (statusLogin == StatusCode.CREDENTIALS.getCode()) {
                sokeeseException.setType(StatusCode.CREDENTIALS);
                throw sokeeseException;
            } else if (statusLogin == StatusCode.ALREADY_LOGGED.getCode()) {
                sokeeseException.setType(StatusCode.ALREADY_LOGGED);
                throw sokeeseException;
            } else if (statusLogin == StatusCode.OTHER.getCode()) {
                sokeeseException.setType(StatusCode.OTHER);
                sokeeseException.setCustomType(this.receiver.readUTF());
                throw sokeeseException;
            }
        }
    }

    protected ScheduledExecutorService getExecutorService () {
        return this.executorService;
    }

    @Override
    public void close () {
        try {
            this.isEnabled = false;
            this._socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ___________________________________________  API USE _________________________________________



    public synchronized void sendOrThrow (final Object object) throws IOException {
        if (this.threadSafe != null)
            this.threadSafe.join();

        this.sender.writeUnshared(object);
        this.sender.flush();
    }

    public void send (final Object object) {
        try {
            this.sendOrThrow(object);
        } catch (final IOException ignored) { }
    }


    // ________________________________ //

    public synchronized void sendOrThrow (final String recipient, final Object object) throws IOException {
        if (this.threadSafe != null)
            this.threadSafe.join();

        SendPacket sendPacket = new SendPacket(recipient, object);
        this.sender.writeUnshared(sendPacket);
        this.sender.flush();
    }

    public void send (final String recipient, final Object object) {
        try {
            this.sendOrThrow(recipient, object);
        } catch (final IOException ignored) { }
    }

    public synchronized void sendOrThrow (final String recipient, final Object object, final Consumer<CatcherClient.ReplyBuilder> consumer) throws IOException {
        if (this.threadSafe != null)
            this.threadSafe.join();

        final SendPacket sendPacket = new SendPacket(recipient, object);

        final CatcherClient.ReplyBuilder replyBuilder = this.catcherClient.getReplyBuilder(
                sendPacket.getId(),
                sendPacket.getRecipient()
        );
        consumer.accept(replyBuilder);

        this.sender.writeUnshared(sendPacket);
        this.sender.flush();
    }

    public void send (final String recipient, final Object object, final Consumer<CatcherClient.ReplyBuilder> consumer) {
        try {
            this.sendOrThrow(recipient, object, consumer);
        } catch (final IOException ignored) { }
    }


    public synchronized void sendOrThrow (final String[] recipients, final Object object, final Consumer<CatcherClient.ReplyBuilder> consumer) throws IOException {
        if (this.threadSafe != null)
            this.threadSafe.join();

        final SendPacket sendPacket = new SendPacket(recipients, object);
        consumer.accept(this.catcherClient.getReplyBuilder(sendPacket.getId(), sendPacket.getRecipient()));

        this.sender.writeUnshared(sendPacket);
        this.sender.flush();
    }

    public void send (final String[] recipients, final Object object, final Consumer<CatcherClient.ReplyBuilder> consumer) {
        try {
            this.sendOrThrow(recipients, object);
        } catch (final IOException ignored) { }
    }

    public synchronized void sendOrThrow (final String[] recipients, final Object object) throws IOException {
        if (this.threadSafe != null)
            this.threadSafe.join();

        SendPacket sendPacket = new SendPacket(recipients, object);
        this.sender.writeUnshared(sendPacket);
        this.sender.flush();
    }

    public void send (final String[] recipients, final Object object) {
        try {
            this.sendOrThrow(recipients, object);
        } catch (final IOException ignored) { }
    }

    // ________________________________ //


    public synchronized void sendOrThrow (final Object... objectArray) throws IOException {
        if (this.threadSafe != null)
            this.threadSafe.join();
        for (final Object obj : objectArray)
            this.sender.writeUnshared(obj);
        this.sender.flush();
    }

    public void send (final Object... objectArray) {
        try {
            this.sendOrThrow(objectArray);
        } catch (final IOException ignored) { }
    }

    public synchronized void sendOrThrow (final String recipient, final Object... objectArray) throws IOException {
        if (this.threadSafe != null)
            this.threadSafe.join();
        for (final Object obj : objectArray) {
            final SendPacket sendPacket = new SendPacket(recipient, obj);
            this.sender.writeUnshared(sendPacket);
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
        return this._socket;
    }

    public <A> void on (final Class<A> clazz, final BiConsumer<A, CatcherClient.RequestData> biConsumer) {
        this.catcherClient.on(clazz, biConsumer);
    }

    public void unregister (final Class<?> clazz) {
        this.catcherClient.unregister(clazz);
    }

    public void unregisterAll () {
        this.catcherClient.unregisterAll();
    }



}
