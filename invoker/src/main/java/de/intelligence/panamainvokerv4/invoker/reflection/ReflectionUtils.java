package de.intelligence.panamainvokerv4.invoker.reflection;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;

import de.intelligence.panamainvokerv4.invoker.annotation.meta.PanamaInterface;
import de.intelligence.panamainvokerv4.invoker.exception.NativeException;
import de.intelligence.panamainvokerv4.invoker.proxy.IProxyManager;

public final class ReflectionUtils {

    public ReflectionUtils() {}

    @SuppressWarnings("unchecked")
    public static <T> T newInstance(Class<?> clazz) {
        try {
            return (T) clazz.getConstructor().newInstance();
        } catch (ReflectiveOperationException ex) {
            throw new NativeException("Failed to initialize class " + clazz.getCanonicalName(), ex);
        }
    }

    public static Annotation getInterfaceAnnotation(Class<?> clazz) {
        final List<Annotation> annotations = Arrays.stream(clazz.getAnnotations())
                .filter(a -> a.annotationType().isAnnotationPresent(PanamaInterface.class))
                .toList();
        if (annotations.size() != 1) {
            throw new NativeException("Class has to be annotated with exactly one panama annotation");
        }
        return annotations.get(0);
    }

    @SuppressWarnings("unchecked")
    public static <T extends IProxyManager> Class<T> getProxyManagerClass(Class<?> clazz) {
        return (Class<T>) getInterfaceAnnotation(clazz).annotationType().getAnnotation(PanamaInterface.class).value();
    }

}
