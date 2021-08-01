package fr.i360matt.sokeese.client;

import fr.i360matt.sokeese.client.exceptions.ClientAlreadyLoggedException;
import fr.i360matt.sokeese.client.exceptions.ClientCodeSentException;
import fr.i360matt.sokeese.client.exceptions.ClientCredentialsException;
import fr.i360matt.sokeese.common.StatusCode;
import fr.i360matt.sokeese.common.redistribute.RawPacket;

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



    public void connect (final String host, final int port, final String username, final String password) throws IOException, ClientCodeSentException, ClientCredentialsException, ClientAlreadyLoggedException, ClassNotFoundException {
        this.connect(new Socket(host, port), username, password);
    }

    public synchronized void connect (final Socket _socket, final String username, final String password) throws IOException, RuntimeException, ClientCodeSentException, ClientCredentialsException, ClientAlreadyLoggedException, ClassNotFoundException {
        this._socket = _socket;
        this.executorService = Executors.newScheduledThreadPool(4);
        this.threadSafe = new CompletableFuture<>();
        try (final Socket socket = _socket) {
            try (final ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {
                this.receiver = ois;
                this.doWaitServerStatus(username, password);
                try (final ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream())) {
                    this.sender = oos;
                    this.doSendCredentials(username, password);
                    this.doWaitLoginStatus(username, password);


                    this.threadSafe.complete(null);
                    this.threadSafe = null;

                    this.isEnabled = true;
                    while (this.isEnabled) {
                        if (!socket.isClosed()) {
                            this.catcherClient.call(this.receiver.readObject());
                            continue;
                        }
                        break;
                    }

                    // normal run here ...
                }
            }
        } catch (final Exception e) {
            if (e instanceof SocketException || e instanceof EOFException)
                return;
            throw e;
        } finally {
            this.executorService.shutdownNow();
            this.executorService = null;
        }
    }

    private void doWaitServerStatus (final String username, final String password) throws IOException, ClientCodeSentException {
        final int statusConnect = this.receiver.readInt();
        String customStatusLogin = "";
        if (statusConnect != 0) {
            if (statusConnect == -1) {
                customStatusLogin = this.receiver.readUTF();
            }
            throw new ClientCodeSentException(statusConnect, customStatusLogin, username, password);
        }
    }

    private void doSendCredentials (final String username, final String password) throws IOException {
        this.sender.writeUTF(username);
        this.sender.writeUTF(password);
        this.sender.flush();
    }

    private void doWaitLoginStatus (final String username, final String password) throws IOException, ClientCredentialsException, ClientCodeSentException, ClientAlreadyLoggedException {
        final int statusLogin = this.receiver.readInt();

        if (statusLogin < 0) {
            if (statusLogin == StatusCode.Login.INVALID_CREDENTIALS) {
                throw new ClientCredentialsException(
                        statusLogin,
                        null,
                        username,
                        password
                );
            } else if (statusLogin == StatusCode.Login.ALREADY_LOGGED) {
                throw new ClientAlreadyLoggedException(
                        statusLogin,
                        null,
                        username
                );
            }

            String customStatusLogin = "";
            if (statusLogin == -1)
                customStatusLogin = this.receiver.readUTF();
            throw new ClientCodeSentException(
                    statusLogin,
                    customStatusLogin,
                    username,
                    password
            );
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



    public void sendOrThrow (final Object object) throws IOException {
        if (this.threadSafe != null)
            this.threadSafe.join();

        this.sender.writeObject(object);
        this.sender.flush();
    }

    public void send (final Object object) {
        try {
            this.sendOrThrow(object);
        } catch (final IOException ignored) { }
    }


    // ________________________________ //

    public void sendOrThrow (final String recipient, final Object object) throws IOException {
        if (this.threadSafe != null)
            this.threadSafe.join();

        RawPacket rawPacket = new RawPacket(recipient, object);
        this.sender.writeObject(rawPacket);
        this.sender.flush();
    }

    public void send (final String recipient, final Object object) {
        try {
            this.sendOrThrow(recipient, object);
        } catch (final IOException ignored) { }
    }

    public void sendOrThrow (final String recipient, final Object object, final Consumer<CatcherClient.ReplyBuilder> consumer) throws IOException {
        if (this.threadSafe != null)
            this.threadSafe.join();

        final RawPacket rawPacket = new RawPacket(recipient, object);
        consumer.accept(this.catcherClient.getReplyBuilder(rawPacket.getId(), rawPacket.getRecipient()));

        this.sender.writeObject(rawPacket);
        this.sender.flush();
    }

    public void send (final String recipient, final Object object, final Consumer<CatcherClient.ReplyBuilder> consumer) {
        try {
            this.sendOrThrow(recipient, object, consumer);
        } catch (final IOException ignored) { }
    }


    public void sendOrThrow (final String[] recipients, final Object object, final Consumer<CatcherClient.ReplyBuilder> consumer) throws IOException {
        if (this.threadSafe != null)
            this.threadSafe.join();

        final RawPacket rawPacket = new RawPacket(recipients, object);
        consumer.accept(this.catcherClient.getReplyBuilder(rawPacket.getId(), rawPacket.getRecipient()));

        this.sender.writeObject(rawPacket);
        this.sender.flush();
    }

    public void send (final String[] recipients, final Object object, final Consumer<CatcherClient.ReplyBuilder> consumer) {
        try {
            this.sendOrThrow(recipients, object);
        } catch (final IOException ignored) { }
    }

    public void sendOrThrow (final String[] recipients, final Object object) throws IOException {
        if (this.threadSafe != null)
            this.threadSafe.join();

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
        if (this.threadSafe != null)
            this.threadSafe.join();
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
        if (this.threadSafe != null)
            this.threadSafe.join();

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
        return this._socket;
    }

    public <A> void on (final Class<A> clazz, final BiConsumer<A, CatcherClient.OnRequest> biConsumer) {
        this.catcherClient.on(clazz, biConsumer);
    }

    public void unregister (final Class<?> clazz) {
        this.catcherClient.unregister(clazz);
    }

    public void unregisterAll () {
        this.catcherClient.unregisterAll();
    }



}
