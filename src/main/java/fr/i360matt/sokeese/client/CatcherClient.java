package fr.i360matt.sokeese.client;


import fr.i360matt.sokeese.common.redistribute.Packet;
import fr.i360matt.sokeese.common.redistribute.reply.SendReply;
import fr.i360matt.sokeese.common.redistribute.reply.Reply;

import java.io.Closeable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class CatcherClient implements Closeable {

    private final SokeeseClient client;

    private final RequestData EMPTY_onRequest = new RequestData(null, -1);
    private final Map<Class<?>, Set<BiConsumer<?, RequestData>>> simpleEvent = new HashMap<>();
    private final Map<Long, Map<Class<?>, ReplyExecuted>> complexEvents = new ConcurrentHashMap<>();

    public CatcherClient (final SokeeseClient client) {
        this.client = client;
    }

    public ReplyBuilder getReplyBuilder (final long idRequest, final Object recipient) {
        return new ReplyBuilder(idRequest, recipient);
    }


    public void incomingRequest (final Object obj) {
        if (obj instanceof Packet) {
            this.asPacket((Packet) obj);
            return;
        }
        if (obj instanceof Reply) {
            this.asReply((Reply) obj);
            return;
        }

        this.callWithRequest(obj, EMPTY_onRequest);
    }

    private void asReply (final Reply reply) {
        final Map<Class<?>, ReplyExecuted> relatedMap = complexEvents.get(reply.getId());
        if (relatedMap != null) {
            // if reply is waited

            final ReplyExecuted candidate = relatedMap.get(reply.getObj().getClass());
            if (candidate != null) {
                // if reply class-type is waited

                candidate.biConsumer.accept(reply.getObj(), reply.getSender());
                candidate.removeToQueue(reply.getSender());
            }
        }
    }


    private void asPacket (final Packet packet) {
        final RequestData onRequest = new RequestData(
                packet.getSender(),
                packet.getIdRequest()
        );

        this.callWithRequest(packet.getObj(), onRequest);
    }



    public <A> void on (final Class<A> clazz, final BiConsumer<A, RequestData> biConsumer) {
        final Set<BiConsumer<?, RequestData>> hashset = simpleEvent.computeIfAbsent(clazz, k -> new HashSet<>());
        hashset.add(biConsumer);
    }

    public void unregister (final Class<?> clazz) {
        simpleEvent.remove(clazz);
    }

    public void unregisterAll () {
        simpleEvent.clear();
    }

    public void callWithRequest (final Object obj, final RequestData onRequest) {
        final Set<BiConsumer<?, RequestData>> hashset = simpleEvent.get(obj.getClass());
        if (hashset != null) {
            for (final BiConsumer<?, RequestData> consumer : hashset) {
                ((BiConsumer) consumer).accept(obj, onRequest);
            }
        }
    }

    public final class RequestData {
        private final String clientName;
        private final long idRequest;

        public RequestData (final String clientName, final long idRequest) {
            this.clientName = clientName;
            this.idRequest = idRequest;
        }

        public String getClientName () {
            return this.clientName;
        }

        public void reply (final Object obj) {
            if (this.idRequest != -1) {
                final SendReply sendReply = new SendReply(
                        this.clientName,
                        obj,
                        this.idRequest
                );
                client.send(sendReply);
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
            if (!client.getExecutorService().isShutdown()) {
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

            if (!client.getExecutorService().isShutdown()) {
                client.getExecutorService().schedule(() -> {
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
            if (!client.getExecutorService().isShutdown()) {
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
    }

    @Override
    public void close () {
        this.complexEvents.clear();
    }
}
