package fr.i360matt.sokeese.server;

import fr.i360matt.sokeese.common.redistribute.SendPacket;
import fr.i360matt.sokeese.common.redistribute.reply.Reply;
import fr.i360matt.sokeese.common.redistribute.reply.SendReply;

import java.io.Closeable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class CatcherServer implements Closeable {

    public static final RequestData EMPTY___REQUEST_DATA = new RequestData(null, -1);

    private final SokeeseServer server;

    private final Map<Class<?>, Set<BiConsumer<?, RequestData>>> simpleEvents = new HashMap<>();
    private final Map<Long, Map<Class<?>, ReplyExecuted>> replyEvents = new ConcurrentHashMap<>();


    public CatcherServer (final SokeeseServer server) {
        this.server = server;
    }


    void replyForServer (final SendReply reply, final LoggedClient client) {
        final Map<Class<?>, ReplyExecuted> relatedMap = replyEvents.get(reply.getId());
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

    void packetForServer (final SendPacket packet, final LoggedClient client) {
        final RequestData requestData = new RequestData(
                client,
                packet.getId()
        );

        this.callWithRequest(packet.getObj(), requestData);
    }


    public <A> void on (final Class<A> clazz, final BiConsumer<A, RequestData> biConsumer) {
        final Set<BiConsumer<?, RequestData>> hashset = simpleEvents.computeIfAbsent(clazz, k -> new HashSet<>());
        hashset.add(biConsumer);
    }

    public void unregister (final Class<?> clazz) {
        simpleEvents.remove(clazz);
    }

    public void unregisterAll () {
        simpleEvents.clear();
    }


    public ReplyBuilder getReplyBuilder (final long idRequest, final Object recipient) {
        return new ReplyBuilder(idRequest, recipient);
    }

    public void callWithRequest (final Object obj, final RequestData requestData) {
        final Set<BiConsumer<?, RequestData>> hashset = simpleEvents.get(obj.getClass());
        if (hashset != null) {
            for (BiConsumer<?, RequestData> consumer : hashset) {
                ((BiConsumer) consumer).accept(obj, requestData);
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

            replyEvents.put(idRequest, this.relatedMap);
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
                        replyEvents.remove(this.idRequest);
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

    public static final class RequestData {
        private final LoggedClient loggedClient;
        private final long idRequest;

        public RequestData (final LoggedClient loggedClient, final long idRequest) {
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





    @Override
    public void close () {
        this.replyEvents.clear();
    }
}
