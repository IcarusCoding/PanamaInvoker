package de.intelligence.panamainvokerv4.invoker.library;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;

import de.intelligence.panamainvokerv4.invoker.Panama;
import de.intelligence.panamainvokerv4.invoker.convert.TypeConverter;
import de.intelligence.panamainvokerv4.invoker.exception.NativeException;
import de.intelligence.panamainvokerv4.invoker.type.Pointer;
import de.intelligence.panamainvokerv4.invoker.update.AutoReadable;
import de.intelligence.panamainvokerv4.invoker.update.AutoWritable;
import de.intelligence.panamainvokerv4.invoker.util.MemoryLayoutUtils;

public final class NativeFunction extends Pointer {

    private final String name;
    private final Method method;
    private final MethodHandle nativeMethodHandle;

    public NativeFunction(MemorySegment segment, Method method, String name, FunctionDescriptor descriptor) {
        super(segment);
        this.method = method;
        this.name = name;
        this.nativeMethodHandle = Linker.nativeLinker().downcallHandle(segment, descriptor);
    }

    public Object invoke(Object[] args) {
        final Object returnValue;
        if (args != null) {
            final Object[] argsCopy = new Object[args.length];
            System.arraycopy(args, 0, argsCopy, 0, args.length);

            this.convertArgsInPlace(argsCopy);

            try {
                returnValue = this.nativeMethodHandle.invokeWithArguments(argsCopy);
            } catch (Throwable ex) {
                throw new NativeException("Failed to invoke native method", ex);
            }
            for (final Object arg : args) {
                if (arg instanceof AutoReadable autoReadable) {
                    autoReadable.autoRead(false);
                }
            }
        } else {
            try {
                returnValue = this.nativeMethodHandle.invoke();
            } catch (Throwable ex) {
                throw new NativeException("Failed to invoke native method", ex);
            }
        }

        final Class<?> returnType = this.method.getReturnType();
        final TypeConverter converter = Panama.getConverters().getConverterInstance(returnType);
        if (converter != null) {
            return converter.toJava(returnValue);
        }
        return returnValue;
    }

    private void convertArgsInPlace(Object[] args) {
        for (int i = 0; i < args.length; i++) {
            final Object arg = args[i];
            if (arg == null) {
                continue;
            }
            final Class<?> argType = arg.getClass();
            final TypeConverter converter = Panama.getConverters().getConverterInstance(argType);
            if (converter != null) {
                args[i] = converter.toNative(arg);
            } else if (!MemoryLayoutUtils.isPrimitiveOrBoxedPrimitive(argType)) {
                throw new NativeException("Cannot convert java type " + argType.getCanonicalName() + " to native type");
            }
            if (arg instanceof AutoWritable autoWritable) {
                autoWritable.autoWrite(true);
            }
        }
    }

    public String getName() {
        return this.name;
    }

}
