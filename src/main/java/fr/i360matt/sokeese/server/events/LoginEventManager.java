package fr.i360matt.sokeese.server.events;


import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class LoginEventManager {

    protected final Set<Consumer<LoginEvent>> LOGIN = new HashSet<>();

    public void execEvent (LoginEvent datas) {
        try {
            do {
                for (final Consumer<LoginEvent> candidate : LOGIN) {
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

    public Set<Consumer<LoginEvent>> getLoginEvents () {
        return this.LOGIN;
    }
}
