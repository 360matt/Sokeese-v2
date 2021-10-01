package fr.i360matt.sokeese.server;

import fr.i360matt.sokeese.common.redistribute.Packet;
import fr.i360matt.sokeese.common.redistribute.SendPacket;
import fr.i360matt.sokeese.common.redistribute.reply.SendReply;
import fr.i360matt.sokeese.common.redistribute.reply.Reply;

import java.io.Closeable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class CatcherServer implements Closeable {

    private final Map<Class<?>, Set<BiConsumer<?, OnRequest>>> simpleEvents = new HashMap<>();
    private final Map<Long, Map<Class<?>, ReplyExecuted>> complexEvents = new ConcurrentHashMap<>();

    private final OnRequest EMPTY_onRequest = new OnRequest(null, -1);

    public static final class OnRequest {
        private final LoggedClient loggedClient;
        private final long idRequest;

        public OnRequest (final LoggedClient loggedClient, final long idRequest) {
            this.loggedClient = loggedClient;
            this.idRequest = idRequest;
        }

        public LoggedClient getClientInstance () {
            return this.loggedClient;
        }

        public String getClientName () {
            return this.loggedClient.getClientName();
        }

        public void reply (final Object obj) {
            if (idRequest != -1) {
                final Reply rawReply = new Reply(
                        "",
                        obj,
                        this.idRequest
                );
                loggedClient.send(rawReply);
            }
        }
    }

    public final class ReplyExecuted {
        private final BiConsumer<Object, LoggedClient> biConsumer;
        private final ReplyBuilder builder;

        public ReplyExecuted (final BiConsumer<Object, LoggedClient> biConsumer, final ReplyBuilder builder) {
            this.biConsumer = biConsumer;
            this.builder = builder;
        }

        public void removeToQueue (final String clientName) {
            if (!server.getExecutorService().isShutdown()) {
                if (this.builder.recipient instanceof List) {
                    ((List) this.builder.recipient).remove(clientName);
                    if (((List) this.builder.recipient).isEmpty()) {
                        this.builder.resultNothing.cancel(true);
                    }
                } else {
                    if (this.builder.resultNothing != null) {
                        this.builder.resultNothing.cancel(true);
                    }
                }
            }
        }
    }

    public final class ReplyBuilder {
        private Map<Class<?>, ReplyExecuted> relatedMap;
        private ScheduledFuture<?> resultNothing;

        private final long idRequest;
        private final Object recipient;

        private int maxDelay;

        public ReplyBuilder (final long idRequest, final Object recipient) {
            this.relatedMap = new ConcurrentHashMap<>();


            this.idRequest = idRequest;
            this.recipient = recipient;

            complexEvents.put(idRequest, this.relatedMap);
        }

        public <A> ReplyBuilder on (final Class<A> clazz, final BiConsumer<Object, LoggedClient> biConsumer) {
            return this.on(clazz, 3000, biConsumer);
        }

        public <A> ReplyBuilder on (final Class<A> clazz, int delay, final BiConsumer<Object, LoggedClient> biConsumer) {
            final ReplyExecuted replyExecuted = new ReplyExecuted(biConsumer, this);
            this.relatedMap.put(clazz, replyExecuted);

            if (delay < 3000)
                delay = 3000;
            if (delay > this.maxDelay)
                maxDelay = delay;

            if (!server.getExecutorService().isShutdown()) {
                server.getExecutorService().schedule(() -> {
                    this.relatedMap.remove(clazz);
                    if (this.relatedMap.isEmpty()) {
                        complexEvents.remove(this.idRequest);
                        this.relatedMap = null;
                    }
                }, delay, TimeUnit.MILLISECONDS);
            }
            return this;
        }

        public void nothing (final Consumer<String> consumer) {
            if (!server.getExecutorService().isShutdown()) {
                this.resultNothing = server.getExecutorService().schedule(() -> {
                    if (this.recipient instanceof List) {
                        for (final String candidate : ((List<String>) this.recipient))
                            consumer.accept(candidate);
                    } else if (this.recipient != null) {
                        consumer.accept((String) this.recipient);
                    }
                }, maxDelay, TimeUnit.MILLISECONDS);
            }
        }
    }

    private final SokeeseServer server;
    public CatcherServer (final SokeeseServer server) {
        this.server = server;
    }
    public ReplyBuilder getReplyBuilder (final long idRequest, final Object recipient) {
        return new ReplyBuilder(idRequest, recipient);
    }


    public <A> void on (final Class<A> clazz, final BiConsumer<A, OnRequest> biConsumer) {
        final Set<BiConsumer<?, OnRequest>> hashset = simpleEvents.computeIfAbsent(clazz, k -> new HashSet<>());
        hashset.add(biConsumer);
    }

    public void unregister (final Class<?> clazz) {
        simpleEvents.remove(clazz);
    }

    public void unregisterAll () {
        simpleEvents.clear();
    }

    public void incomingRequest (final Object obj, final LoggedClient user) {
        if (obj instanceof SendPacket) {
            this.processRawPacket((SendPacket) obj, user);
            return;
        }

        if (obj instanceof SendReply) {
            this.processRawReply((SendReply) obj, user);
            return;
        }

        this.processDirectObject(obj);

    }

    public void processDirectObject (final Object obj) {
        this.callWithRequest(obj, EMPTY_onRequest);
    }

    public void callWithRequest (final Object obj, final OnRequest onRequest) {
        final Set<BiConsumer<?, OnRequest>> hashset = simpleEvents.get(obj.getClass());
        if (hashset != null) {
            for (BiConsumer<?, OnRequest> consumer : hashset) {
                ((BiConsumer) consumer).accept(obj, onRequest);
            }
        }
    }


    private void processReply (final SendReply reply, final LoggedClient client) {
        final Map<Class<?>, ReplyExecuted> relatedMap = complexEvents.get(reply.getId());
        if (relatedMap != null) {
            // if reply is waited

            final ReplyExecuted candidate = relatedMap.get(reply.getObj().getClass());
            if (candidate != null) {
                // if reply class-type is waited

                candidate.biConsumer.accept(reply.getObj(), client);
                candidate.removeToQueue(client.getClientName());
            }
        }
    }

    private void processPacket (final SendPacket packet, final LoggedClient client) {
        final OnRequest onRequest = new OnRequest(
                client,
                packet.getId()
        );

        this.callWithRequest(packet.getObj(), onRequest);
    }

    private void processRawReply (final SendReply sendReply, final LoggedClient client) {
        if (sendReply.getObj() instanceof SendPacket || sendReply.getObj() instanceof Packet) {
            return;
            // fixed exploit/bug: infinite recursive.
        }


        if (sendReply.getRecipient() instanceof String) {
            final LoggedClient candidate = server.getClient((String) sendReply.getRecipient());
            if (candidate != null) {
                final Reply reply = new Reply(
                        client.getClientName(),
                        sendReply.getObj(),
                        sendReply.getId()
                );
                candidate.send(reply);
            }
        } else {
            this.processReply(sendReply, client);
        }
    }

    private void processRawPacket (final SendPacket sendPacket, final LoggedClient user) {
        if (sendPacket.getObj() instanceof SendPacket || sendPacket.getObj() instanceof Packet) {
            return;
            // fixed exploit/bug: infinite recursive.
        }

        if (sendPacket.getRecipient() instanceof String) {
            final String recipient = (String) sendPacket.getRecipient();
            if (recipient.equals("")) {
                // empty recipient = the recipient is server
                this.processPacket(sendPacket, user);
                return;
            }

            if (recipient.equals("*")) {
                final Packet packet = new Packet(
                        sendPacket.getObj(),
                        user.getClientName(),
                        sendPacket.getId()
                );

                for (final LoggedClient client : this.server.getClientsManager().getInstances())
                    client.send(packet);
            } else if (recipient.equals("**")) {
                final Packet packet = new Packet(
                        sendPacket.getObj(),
                        user.getClientName(),
                        sendPacket.getId()
                );

                for (final LoggedClient client : this.server.getClientsManager().getInstances())
                    client.send(packet);

                this.processPacket(sendPacket, user);
            } else {
                final LoggedClient candidate = server.getClient(recipient);
                if (candidate != null) {
                    final Packet packet = new Packet(
                            sendPacket.getObj(),
                            user.getClientName(),
                            sendPacket.getId()
                    );
                    candidate.send(packet);
                }
            }
        } else if (sendPacket.getRecipient() instanceof String[]) {
            final String[] fixedRecipient = (String[]) Arrays.stream((String[]) sendPacket.getRecipient()).distinct().toArray();
            // fixed exploit/bug: amplified DDoS with multiple same client name.

            Packet packet = null;

            for (final String candidateName : fixedRecipient) {
                if (candidateName == null) {
                    this.processPacket(sendPacket, user);
                    continue;
                }

                final LoggedClient candidate = server.getClient(candidateName);
                if (candidate != null) {
                    if (packet == null) {
                        packet = new Packet(
                                sendPacket.getObj(),
                                user.getClientName(),
                                sendPacket.getId()
                        );
                    }
                    candidate.send(packet);
                }
            }
        } else {
            // empty recipient = the recipient is server
            this.processPacket(sendPacket, user);
        }

    }

    @Override
    public void close () {
        this.complexEvents.clear();
    }
}
