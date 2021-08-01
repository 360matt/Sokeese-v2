package fr.i360matt.sokeese.server;

import fr.i360matt.sokeese.common.EventAbstract;
import fr.i360matt.sokeese.server.events.LoginEvent;
import fr.i360matt.sokeese.server.events.PreLoginEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class EventsServer {

    public <D extends EventAbstract> void execEvent (final Set<? extends Consumer<D>> events, D datas, BiConsumer<Integer, String> consumer) {
        do {
            for (final Consumer<D> candidate : events) {
                if (datas.getStatusCode() < 0)
                    break;
                candidate.accept(datas);
            }
            datas.reLoop(false);
        } while (datas.getReLoop());


        if (datas.getFreeeze() > 0) {
            try {
                Thread.sleep(datas.getFreeeze());
            } catch (final Exception ignored) {}
        }

        consumer.accept(datas.getStatusCode(), datas.getStatusCustom());
    }


    private final Set<Consumer<PreLoginEvent>> PRELOGIN = new HashSet<>();
    private final Set<Consumer<LoginEvent>> LOGIN = new HashSet<>();

    public Set<Consumer<PreLoginEvent>> getPreLogin () {
        return this.PRELOGIN;
    }

    public Set<Consumer<LoginEvent>> getLogin () {
        return this.LOGIN;
    }
}
