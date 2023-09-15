package de.intelligence.panamainvokerv4.invoker.library;

import java.lang.foreign.MemorySegment;
import java.lang.reflect.Method;
import java.util.Arrays;

import de.intelligence.panamainvokerv4.invoker.type.Pointer;
import de.intelligence.panamainvokerv4.invoker.util.DynamicMethodHandle;

public final class NativeFunction extends Pointer {

    private final Method method;
    private final String name;

    public NativeFunction(MemorySegment segment, Method method) {
        super(segment);
        this.method = method;
        this.name = method.getName();
    }

    public NativeFunction(Pointer funcPtr) {
        super(funcPtr.getSegment());
        this.method = null;
        this.name = funcPtr.toString();
    }

    public static NativeFunction fromAddress(Pointer funcPtr) {
        return new NativeFunction(funcPtr);
    }

    public static NativeFunction fromAddress(long address) {
        return new NativeFunction(new Pointer(address));
    }

    public Object invoke(Class<?> retType, Object... args) {
        final Object[] argsCopy = new Object[args == null ? 0 : args.length];
        Class<?>[] classTypes = new Class[argsCopy.length];
        if (args != null) {
            System.arraycopy(args, 0, argsCopy, 0, args.length);
            classTypes = Arrays.stream(argsCopy).map(Object::getClass).toArray(Class<?>[]::new);
        }
        if (this.method != null) {
            return DynamicMethodHandle.auto(super.segment, this.method).invokeWithArguments(argsCopy);
        }
        return DynamicMethodHandle.auto(this.segment, this.name, retType, classTypes, false).invokeWithArguments(argsCopy);
    }

    public String getName() {
        return this.name;
    }

}
