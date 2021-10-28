package fr.i360matt.sokeese.server.events;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;

public abstract class Listener {

    protected Method[] methods;

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Event { }



}
