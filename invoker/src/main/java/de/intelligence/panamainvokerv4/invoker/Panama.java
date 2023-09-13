package de.intelligence.panamainvokerv4.invoker;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import de.intelligence.panamainvokerv4.invoker.convert.DefaultTypeConverters;
import de.intelligence.panamainvokerv4.invoker.convert.ITypeConverters;
import de.intelligence.panamainvokerv4.invoker.exception.NativeException;
import de.intelligence.panamainvokerv4.invoker.proxy.IProxyManager;
import de.intelligence.panamainvokerv4.invoker.reflection.ReflectionUtils;

public final class Panama {

    private static final Map<Class<? extends Annotation>, IProxyManager> MANAGERS;
    private static final ITypeConverters DEFAULT_CONVERTERS = new DefaultTypeConverters();

    static {
        MANAGERS = new HashMap<>();
    }

    private Panama() {}

    // load by using caller class
    @SuppressWarnings("unchecked")
    public static <T> T load() {
        final StackWalker walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
        return (T) load(walker.getCallerClass());
    }

    // load by interface parameter
    public static <T> T load(Class<T> interfaceClass) {
        if (!interfaceClass.isInterface()) {
            throw new NativeException("Class " + interfaceClass.getCanonicalName() + " must be an interface");
        }
        final Class<? extends IProxyManager> proxyManagerClass = ReflectionUtils.getProxyManagerClass(interfaceClass);
        final Class<? extends Annotation> annotationClass = ReflectionUtils.getInterfaceAnnotation(interfaceClass).annotationType();
        final IProxyManager proxyManager;
        synchronized (MANAGERS) {
            proxyManager = MANAGERS.computeIfAbsent(annotationClass, aC -> ReflectionUtils.newInstance(proxyManagerClass));
        }
        return interfaceClass.cast(proxyManager.createProxy(interfaceClass));
    }

    public static ITypeConverters getConverters() {
        return DEFAULT_CONVERTERS;
    }

}
