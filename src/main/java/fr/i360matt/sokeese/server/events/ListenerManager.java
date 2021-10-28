package fr.i360matt.sokeese.server.events;

import fr.i360matt.sokeese.server.SokeeseServer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

public class ListenerManager {

    private final SokeeseServer server;
    private final Set<Listener> listeners = new HashSet<>();

    public ListenerManager (SokeeseServer server) {
        this.server = server;
    }

    public ListenerManager registerListener (final Listener listener) {
        final Set<Method> methods = new HashSet<>();
        for (final Method method : listener.getClass().getDeclaredMethods()) {
            method.setAccessible(true);

            final Class<?>[] parameters = method.getParameterTypes();
            if (parameters.length != 1)
                continue;

            if (!Event.class.isAssignableFrom(parameters[0]))
                continue;

            Listener.Event annot = method.getAnnotation(Listener.Event.class);
            if (annot != null) {
                methods.add(method);
            }
        }
        listener.methods = methods.toArray(new Method[0]);
        listeners.add(listener);

        return this;
    }

    public ListenerManager removeListener (final Listener listener) {
        listeners.remove(listener);
        return this;
    }

    public ListenerManager removeAllListeners () {
        listeners.clear();
        return this;
    }

    public ListenerManager callEvent (final Event event) {
        for (final Listener listener : listeners) {
            do {
                for (final Method method : listener.methods) {
                    try {
                        method.invoke(listener, event);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
            } while (event.getLoop());
        }
        return this;
    }

}
