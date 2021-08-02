package fr.i360matt.sokeese.server;

import fr.i360matt.sokeese.common.redistribute.Packet;
import fr.i360matt.sokeese.common.redistribute.RawPacket;
import fr.i360matt.sokeese.common.redistribute.reply.RawReply;
import fr.i360matt.sokeese.common.redistribute.reply.Reply;

import java.io.Closeable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class CatcherServer implements Closeable {

    private final Map<Class<?>, Set<BiConsumer<?, OnRequest>>> simpleEvents = new HashMap<>();
    private final Map<Long, Map<Class<?>, ReplyExecuted>> complexEvents = new ConcurrentHashMap<>();

    protected final OnRequest EMPTY_onRequest = new OnRequest(null, -1);

    public static class OnRequest {
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
            if (this.builder.recipient instanceof List) {
                ((List) this.builder.recipient).remove(clientName);
            } else {
                // this.builder.recipient = null;
                if (this.builder.resultNothing != null) {
                    this.builder.resultNothing.cancel(true);
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

            server.getExecutorService().schedule(() -> {
                this.relatedMap.remove(clazz);
                if (this.relatedMap.isEmpty()) {
                    complexEvents.remove(this.idRequest);
                    this.relatedMap = null;
                }
            }, delay, TimeUnit.MILLISECONDS);
            return this;
        }

        public void nothing (final Consumer<String> consumer) {
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

    private final SokeeseServer server;
    public CatcherServer (final SokeeseServer server) {
        this.server = server;
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

    public <A> void incomingRequest (final A obj, final LoggedClient user) {
        if (obj instanceof RawPacket) {
            this.processRawPacket((RawPacket) obj, user);
            return;
        }

        if (obj instanceof RawReply) {
            this.processRawReply((RawReply) obj, user);
            return;
        }

        this.processDirectObject(obj);

    }

    public <A> void processDirectObject (final A obj) {
        this.callWithRequest(obj, EMPTY_onRequest);
    }

    public <A> void callWithRequest (final A obj, final OnRequest onRequest) {
        final Set<BiConsumer<?, OnRequest>> hashset = simpleEvents.get(obj.getClass());
        if (hashset != null) {
            for (BiConsumer<?, OnRequest> consumer : hashset) {
                ((BiConsumer<A, OnRequest>) consumer).accept(obj, onRequest);
            }
        }
    }


    private void processReply (final RawReply reply, final LoggedClient client) {
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

    private void processPacket (final RawPacket packet, final LoggedClient client) {
        final OnRequest onRequest = new OnRequest(
                client,
                packet.getId()
        );

        this.callWithRequest(packet.getObj(), onRequest);
    }

    private void processRawReply (final RawReply rawReply, final LoggedClient client) {
        if (rawReply.getObj() instanceof RawPacket || rawReply.getObj() instanceof Packet) {
            return;
            // fixed exploit/bug: infinite recursive.
        }


        if (rawReply.getRecipient() instanceof String) {
            final LoggedClient candidate = server.getClient((String) rawReply.getRecipient());
            if (candidate != null) {
                final Reply reply = new Reply(
                        client.getClientName(),
                        rawReply.getObj(),
                        rawReply.getId()
                );
                candidate.send(reply);
            }
        } else {
            this.processReply(rawReply, client);
        }
    }

    private void processRawPacket (final RawPacket rawPacket, final LoggedClient user) {
        if (rawPacket.getObj() instanceof RawPacket || rawPacket.getObj() instanceof Packet) {
            return;
            // fixed exploit/bug: infinite recursive.
        }

        if (rawPacket.getRecipient() instanceof String) {
            final String recipient = (String) rawPacket.getRecipient();
            if (recipient.equals("")) {
                // empty recipient = the recipient is server
                this.processPacket(rawPacket, user);
                return;
            }

            final LoggedClient candidate = server.getClient(recipient);
            if (candidate != null) {
                final Packet packet = new Packet(
                        rawPacket.getObj(),
                        user.getClientName(),
                        rawPacket.getId()
                );
                candidate.send(packet);
            }
        } else if (rawPacket.getRecipient() instanceof String[]) {
            final String[] fixedRecipient = (String[]) Arrays.stream((String[]) rawPacket.getRecipient()).distinct().toArray();
            // fixed exploit/bug: amplified DDoS with multiple same client name.

            Packet packet = null;

            for (final String candidateName : fixedRecipient) {
                if (candidateName == null) {
                    this.processPacket(rawPacket, user);
                    continue;
                }

                final LoggedClient candidate = server.getClient(candidateName);
                if (candidate != null) {
                    if (packet == null) {
                        packet = new Packet(
                                rawPacket.getObj(),
                                user.getClientName(),
                                rawPacket.getId()
                        );
                    }
                    candidate.send(packet);
                }
            }
        } else {
            // empty recipient = the recipient is server
            this.processPacket(rawPacket, user);
        }

    }

    @Override
    public void close () {
        this.complexEvents.clear();
    }
}
