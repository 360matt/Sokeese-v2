package fr.i360matt.sokeese.server.events;


import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class ServerEventManager {

    protected final Set<Consumer<LoginEvent>> LOGIN = new HashSet<>();

    public <T extends EventAbstract> void execEvent (final Set<Consumer<T>> events, T datas) {
        try {
            do {
                for (final Consumer<T> candidate : events) {
                    if (datas.getStatusCode().getCode() >= 0) {
                        candidate.accept(datas);
                        continue;
                    }
                    break;
                }
                datas.reLoop(false);
            } while (datas.getReLoop());

            if (datas.getFreeeze() > 0) {
                Thread.sleep(datas.getFreeeze());
            }

            datas.callback();
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    public Set<Consumer<LoginEvent>> getLogin () {
        return this.LOGIN;
    }
}
