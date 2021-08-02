package fr.i360matt.sokeese.server;

import fr.i360matt.sokeese.common.redistribute.Packet;
import fr.i360matt.sokeese.common.redistribute.RawPacket;
import fr.i360matt.sokeese.common.redistribute.reply.RawReply;
import fr.i360matt.sokeese.common.redistribute.reply.Reply;

import java.io.Closeable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class CatcherServer implements Closeable {

    private final Map<Class<?>, Set<BiConsumer<?, OnRequest>>> simpleEvents = new ConcurrentHashMap<>();
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
                final RawReply rawReply = new RawReply(
                        this.loggedClient.getClientName(),
                        obj,
                        this.idRequest
                );
                loggedClient.send(rawReply);
            }
        }
    }

    public class ReplyExecuted {
        final BiConsumer<Object, LoggedClient> biConsumer;
        final ReplyBuilder builder;

        public ReplyExecuted (final BiConsumer<Object, LoggedClient> biConsumer, final ReplyBuilder builder) {
            this.biConsumer = biConsumer;
            this.builder = builder;
        }

        public void removeToQueue (final String clientName) {
            if (this.builder.recipIsList) {
                this.builder.recipientList.remove(clientName);
            } else {
                this.builder.recipientString = null;
            }
        }
    }

    public class ReplyBuilder {
        private final Map<Class<?>, ReplyExecuted> relatedMap;

        private final boolean recipIsList;

        private String recipientString;
        private List<String> recipientList;

        private int maxDelay;

        public ReplyBuilder (final long idRequest, final Object recipient) {
            this.relatedMap = new ConcurrentHashMap<>();

            if (recipient instanceof String) {
                this.recipIsList = false;
                this.recipientString = (String) recipient;
            } else {
                this.recipIsList = true;
                this.recipientList = new ArrayList<>(Arrays.asList((String[]) recipient));
            }

            complexEvents.put(idRequest, this.relatedMap);
        }

        public <A> ReplyBuilder on (final Class<A> clazz, final BiConsumer<Object, LoggedClient> biConsumer) {
            return this.on(clazz, 200, biConsumer);
        }

        public <A> ReplyBuilder on (final Class<A> clazz, int delay, final BiConsumer<Object, LoggedClient> biConsumer) {
            final ReplyExecuted replyExecuted = new ReplyExecuted(biConsumer, this);
            this.relatedMap.put(clazz, replyExecuted);

            if (delay > this.maxDelay)
                maxDelay = delay;

            server.getExecutorService().schedule(() -> {
                this.relatedMap.remove(clazz);
            }, delay, TimeUnit.MILLISECONDS);
            return this;
        }

        public void nothing (final Consumer<String> consumer) {
            server.getExecutorService().schedule(() -> {
                if (this.recipIsList) {
                    for (final String candidate : this.recipientList)
                        consumer.accept(candidate);
                } else if (this.recipientString != null) {
                    consumer.accept(this.recipientString);
                }
            }, this.maxDelay*1000L + 100L, TimeUnit.MICROSECONDS);
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

    public void unregisterAll (final Class<?> clazz) {
        simpleEvents.clear();
    }

    public <A> void call (final A obj, final LoggedClient user) {
        if (obj instanceof RawPacket) {
            this.processRawPacket((RawPacket) obj, user);
            return;
        }

        if (obj instanceof RawReply) {
            this.processRawReply((RawReply) obj, user);
            return;
        }

        final Set<BiConsumer<?, OnRequest>> hashset = simpleEvents.get(obj.getClass());
        if (hashset != null) {
            final OnRequest onRequest = new OnRequest(user, -1);
            for (final BiConsumer<?, OnRequest> consumer : hashset) {
                ((BiConsumer<A, OnRequest>) consumer).accept(obj, onRequest);
            }
        }
    }


    private void processReply (final Reply reply, final LoggedClient client) {
        final Map<Class<?>, ReplyExecuted> relatedMap = complexEvents.get(reply.getId());
        if (relatedMap != null) {
            // if reply is waited

            ReplyExecuted candidate = relatedMap.get(reply.getObj().getClass());
            if (candidate != null) {
                // if reply class-type is waited

                candidate.removeToQueue(reply.getSender());
                candidate.biConsumer.accept(reply.getObj(), client);

                relatedMap.remove(reply.getObj().getClass());
            }
        }
    }

    private void processRawReply (final RawReply rawPacket, final LoggedClient client) {
        if (rawPacket.getObj() instanceof RawPacket || rawPacket.getObj() instanceof Packet) {
            return;
            // fixed exploit/bug: infinite recursive.
        }

        if (rawPacket.getRecipient() != null) {
            if (rawPacket.getRecipient() instanceof String) {
                final String recipient = (String) rawPacket.getRecipient();
                if (recipient.equals("")) {
                    // empty recipient = the recipient is server

                    final Reply reply = new Reply(
                            client.getClientName(),
                            rawPacket.getObj(),
                            rawPacket.getId()
                    );
                    this.processReply(reply, client);
                } else {
                    final LoggedClient candidate = server.getClient(recipient);
                    if (candidate != null) {
                        final Reply reply = new Reply(
                                client.getClientName(),
                                rawPacket.getObj(),
                                rawPacket.getId()
                        );

                        candidate.send(reply);
                    }
                }
            }
        }


    }

    private void processRawPacket (final RawPacket rawPacket, final LoggedClient user) {
        if (rawPacket.getObj() instanceof RawPacket || rawPacket.getObj() instanceof Packet) {
            return;
            // fixed exploit/bug: infinite recursive.
        }

        if (rawPacket.getRecipient() != null) {
            if (rawPacket.getRecipient() instanceof String) {
                final String recipient = (String) rawPacket.getRecipient();
                if (recipient.equals("")) {
                    // empty recipient = the recipient is server
                    this.call(rawPacket.getObj(), user);
                } else {
                    final LoggedClient candidate = server.getClient(recipient);
                    if (candidate != null) {
                        final Packet packet = new Packet(
                                rawPacket.getObj(),
                                user.getClientName(),
                                rawPacket.getId()
                        );
                        candidate.send(packet);
                    }
                }
            } else if (rawPacket.getRecipient() instanceof String[]) {
                final String[] fixedRecipient = (String[]) Arrays.stream((String[]) rawPacket.getRecipient()).distinct().toArray();
                // fixed exploit/bug: amplified DDoS with multiple same client name.

                for (final String candidateName : fixedRecipient) {
                    if (candidateName == null)
                        continue;
                    if (candidateName.equals("")) {
                        this.call(rawPacket.getObj(), user);
                        continue;
                    }

                    final LoggedClient candidate = server.getClient(candidateName);
                    if (candidate != null) {
                        final Packet packet = new Packet(
                                rawPacket.getObj(),
                                user.getClientName(),
                                rawPacket.getId()
                        );
                        candidate.send(packet);
                    }
                }
            }
        }
    }

    @Override
    public void close () {
        this.complexEvents.clear();
    }
}
