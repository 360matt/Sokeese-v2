package fr.i360matt.sokeese.client;


import fr.i360matt.sokeese.common.redistribute.Packet;
import fr.i360matt.sokeese.common.redistribute.reply.RawReply;
import fr.i360matt.sokeese.common.redistribute.reply.Reply;

import java.io.Closeable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class CatcherClient implements Closeable {

    private final Map<Class<?>, Set<BiConsumer<?, OnRequest>>> simpleEvent = new HashMap<>();
    private final Map<Long, Map<Class<?>, ReplyExecuted>> complexEvents = new ConcurrentHashMap<>();

    protected final OnRequest EMPTY_onRequest = new OnRequest(null, -1);

    public final class OnRequest {
        private final String senderName;
        private final long idRequest;

        public OnRequest (final String senderName, final long idRequest) {
            this.senderName = senderName;
            this.idRequest = idRequest;
        }

        public String getSenderName () {
            return this.senderName;
        }

        public void reply (final Object obj) {
            if (this.idRequest != -1) {
                final RawReply rawReply = new RawReply(
                        this.senderName,
                        obj,
                        this.idRequest
                );
                client.send(rawReply);
            }
        }
    }

    public final class ReplyExecuted {
        private final BiConsumer<Object, String> biConsumer;
        private final ReplyBuilder builder;

        public ReplyExecuted (final BiConsumer<Object, String> biConsumer, final ReplyBuilder builder) {
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
        private Map<Class<?>, ReplyExecuted> relatedMap = new ConcurrentHashMap<>();
        private ScheduledFuture<?> resultNothing;

        private final long idRequest;
        private final Object recipient;

        private int maxDelay;

        public ReplyBuilder (final long idRequest, final Object recipient) {
            this.idRequest = idRequest;
            this.recipient = recipient;

            complexEvents.put(idRequest, this.relatedMap);
        }

        public <A> ReplyBuilder on (final Class<A> clazz, final BiConsumer<Object, String> biConsumer) {
            return this.on(clazz, 3000, biConsumer);
        }

        public <A> ReplyBuilder on (final Class<A> clazz, int delay, final BiConsumer<Object, String> biConsumer) {
            final ReplyExecuted replyExecuted = new ReplyExecuted(biConsumer, this);
            this.relatedMap.put(clazz, replyExecuted);

            if (delay < 3000)
                delay = 3000;
            if (delay > this.maxDelay)
                maxDelay = delay;

            client.getExecutorService().schedule(() -> {
                this.relatedMap.remove(clazz);
                if (this.relatedMap.isEmpty()) {
                    complexEvents.remove(this.idRequest);
                    this.relatedMap = null;
                }
            }, delay, TimeUnit.MILLISECONDS);
            return this;
        }

        public void nothing (final Consumer<String> consumer) {
            this.resultNothing = client.getExecutorService().schedule(() -> {
                if (this.recipient instanceof List) {
                    for (final String candidate : ((List<String>) this.recipient))
                        consumer.accept(candidate);
                } else if (this.recipient != null) {
                    consumer.accept((String) this.recipient);
                }
            }, maxDelay, TimeUnit.MILLISECONDS);
        }
    }


    private final SokeeseClient client;
    public CatcherClient (final SokeeseClient client) {
        this.client = client;
    }

    public ReplyBuilder getReplyBuilder (final long idRequest, final Object recipient) {
        return new ReplyBuilder(idRequest, recipient);
    }


    public <A> void on (final Class<A> clazz, final BiConsumer<A, OnRequest> biConsumer) {
        final Set<BiConsumer<?, OnRequest>> hashset = simpleEvent.computeIfAbsent(clazz, k -> new HashSet<>());
        hashset.add(biConsumer);
    }

    public void unregister (final Class<?> clazz) {
        simpleEvent.remove(clazz);
    }

    public void unregisterAll () {
        simpleEvent.clear();
    }

    public <A> void incomingRequest (final A obj) {
        if (obj instanceof Packet) {
            this.processPacket((Packet) obj);
            return;
        }
        if (obj instanceof Reply) {
            this.processReply((Reply) obj);
            return;
        }

        this.callWithRequest(obj, EMPTY_onRequest);
    }

    public <A> void callWithRequest (final A obj, final OnRequest onRequest) {
        final Set<BiConsumer<?, OnRequest>> hashset = simpleEvent.get(obj.getClass());
        if (hashset != null) {
            for (final BiConsumer<?, OnRequest> consumer : hashset) {
                ((BiConsumer<A, OnRequest>) consumer).accept(obj, onRequest);
            }
        }
    }

    private void processReply (final Reply reply) {
        final Map<Class<?>, ReplyExecuted> relatedMap = complexEvents.get(reply.getId());
        if (relatedMap != null) {
            // if reply is waited

            final ReplyExecuted candidate = relatedMap.get(reply.getObj().getClass());
            if (candidate != null) {
                // if reply class-type is waited

                candidate.removeToQueue(reply.getSender());
                candidate.biConsumer.accept(reply.getObj(), reply.getSender());
            }
        }
    }


    private void processPacket (final Packet packet) {
        final OnRequest onRequest = new OnRequest(
                packet.getSender(),
                packet.getIdRequest()
        );

        this.callWithRequest(packet.getObj(), onRequest);
    }

    @Override
    public void close () {
        this.complexEvents.clear();
    }
}
