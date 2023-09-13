package de.intelligence.panamainvokerv4.invoker.proxy;

import java.lang.foreign.SegmentScope;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import de.intelligence.panamainvokerv4.invoker.annotation.NativeInterface;
import de.intelligence.panamainvokerv4.invoker.exception.NativeException;
import de.intelligence.panamainvokerv4.invoker.library.NativeLibrary;

public final class NativeInterfaceProxyManager implements IProxyManager {

    @Override
    public Object createProxy(Class<?> interfaceClass) {
        final String libraryName = interfaceClass.getAnnotation(NativeInterface.class).value();
        if (libraryName.isBlank()) {
            throw new NativeException("No native library was specified for native interface " + interfaceClass.getCanonicalName());
        }
        final NativeLibrary nativeLibrary = new NativeLibrary(libraryName, SegmentScope.auto());
        for (final Method declaredMethod : interfaceClass.getDeclaredMethods()) {
            nativeLibrary.registerMethod(declaredMethod);
        }
        return Proxy.newProxyInstance(interfaceClass.getClassLoader(),
                new Class<?>[]{interfaceClass}, new NativeInvocationHandler(interfaceClass, nativeLibrary));
    }

    private static final class NativeInvocationHandler extends InvocationHandlerBase {

        private final NativeLibrary nativeLibrary;

        public NativeInvocationHandler(Class<?> interfaceClass, NativeLibrary nativeLibrary) {
            super(interfaceClass);
            this.nativeLibrary = nativeLibrary;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.equals(Object.class.getMethod("toString"))) {
                return super.interfaceClass.getCanonicalName() + "$PanamaProxy";
            }
            if (method.getDeclaringClass() == Object.class) {
                return method.invoke(this, args);
            }
            return this.nativeLibrary.getFunction(method).invoke(args);
        }

    }

}
