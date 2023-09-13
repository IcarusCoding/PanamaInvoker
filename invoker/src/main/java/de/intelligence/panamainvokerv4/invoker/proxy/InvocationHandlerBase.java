package de.intelligence.panamainvokerv4.invoker.proxy;

import java.lang.reflect.InvocationHandler;

public abstract class InvocationHandlerBase implements InvocationHandler {

    protected final Class<?> interfaceClass;

    protected InvocationHandlerBase(Class<?> interfaceClass) {
        this.interfaceClass = interfaceClass;
    }

    public final Class<?> getInterfaceClass() {
        return this.interfaceClass;
    }

}
