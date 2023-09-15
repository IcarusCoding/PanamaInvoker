package de.intelligence.panamainvokerv4.invoker.util;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import de.intelligence.panamainvokerv4.invoker.exception.NativeException;
import de.intelligence.panamainvokerv4.invoker.update.AutoReadable;
import de.intelligence.panamainvokerv4.invoker.update.AutoWritable;

public final class DynamicMethodHandle {

    private static final Map<MemorySegment, DynamicMethodHandle> HANDLE_CACHE;

    static {
        HANDLE_CACHE = new HashMap<>();
    }

    private final MethodHandler methodHandler;
    private final MethodHandle methodHandle;

    private DynamicMethodHandle(MethodHandler methodHandler, MethodHandle methodHandle) {
        this.methodHandler = methodHandler;
        this.methodHandle = methodHandle;
    }

    public Object invokeWithArguments(Object... arguments) {
        try {
            return this.methodHandle.invokeWithArguments(arguments);
        } catch (Throwable ex) {
            throw new NativeException("Failed to invoke native method", ex);
        }
    }

    private static DynamicMethodHandle computeOrCache(MemorySegment segment, Supplier<DynamicMethodHandle> handleSupplier) {
        synchronized (HANDLE_CACHE) {
            return HANDLE_CACHE.computeIfAbsent(segment, m -> handleSupplier.get());
        }
    }

    public static DynamicMethodHandle auto(MemorySegment segment, Method method) {
        return DynamicMethodHandle.auto(segment, method.getName(), method.getReturnType(), method.getParameterTypes(), method.isVarArgs());
    }

    public static DynamicMethodHandle auto(MemorySegment segment, String name, Class<?> retType, Class<?>[] paramTypes, boolean varArgs) {
        final Supplier<DynamicMethodHandle> handleSupplier = () -> {
            // 1. validate method
            if (!ConversionUtils.isTypeSupported(retType)) {
                throw new NativeException("Return type " + retType.getCanonicalName() + " of method " + name + " is unsupported");
            }
            if (paramTypes.length > 0 && varArgs) {
                final Class<?> componentType = paramTypes[paramTypes.length - 1].getComponentType();
                if (!ConversionUtils.isTypeSupported(componentType)) {
                    throw new NativeException("Component type " + componentType.getCanonicalName() + " of method " + name + " is unsupported");
                }
            }
            // 2. create appropriate method handler
            final int numLayouts = varArgs ? paramTypes.length - 1 : paramTypes.length;
            final MemoryLayout[] parameterLayouts = new MemoryLayout[numLayouts];
            for (int i = 0; i < numLayouts; i++) {
                if (!ConversionUtils.isTypeSupported(paramTypes[i])) {
                    throw new NativeException("Parameter type " + paramTypes[i].getCanonicalName() + " of method " + name + " is unsupported");
                }
                parameterLayouts[i] = ConversionUtils.createMemoryLayout(paramTypes[i]);
            }
            final FunctionDescriptor descriptor;
            if (Void.TYPE.equals(retType)) {
                descriptor = FunctionDescriptor.ofVoid(parameterLayouts);
            } else {
                descriptor = FunctionDescriptor.of(ConversionUtils.createMemoryLayout(retType), parameterLayouts);
            }
            final MethodType methodType = MethodType.methodType(retType, paramTypes);
            if (varArgs) {
                return createVariadic(segment, methodType, descriptor);
            } else {
                return createAutoConverter(segment, methodType, descriptor);
            }
        };
        return computeOrCache(segment, handleSupplier);
    }

    //TODO use of custom allocator someday
    public static final SegmentAllocator DEFAULT = (byteSize, byteAlignment) -> null;

    public static DynamicMethodHandle createAutoConverter(MemorySegment segment, MethodType methodType, FunctionDescriptor functionDescriptor) {
        final MethodHandler methodHandler = new AutoConverterHandler(segment, functionDescriptor, methodType.returnType());
        final MethodHandle targetHandle = MethodHandles.insertArguments(MethodHandler.Helper.INVOKE.bindTo(methodHandler), 0, DEFAULT)
                .asCollector(Object[].class, methodType.parameterCount());
        return new DynamicMethodHandle(methodHandler, targetHandle.asType(methodType));
    }

    public static DynamicMethodHandle createVariadic(MemorySegment segment, MethodType methodType, FunctionDescriptor functionDescriptor) {
        final MethodHandler methodHandler = new VariadicHandler(segment, functionDescriptor, methodType.returnType());
        final MethodHandle targetHandle = MethodHandles.insertArguments(MethodHandler.Helper.INVOKE.bindTo(methodHandler)
                .asCollector(Object[].class, functionDescriptor.argumentLayouts().size() + 1), 0, DEFAULT);
        methodType = methodType.dropParameterTypes(methodType.parameterCount() - 1, methodType.parameterCount())
                .appendParameterTypes(Object[].class);

        return new DynamicMethodHandle(methodHandler, targetHandle.asType(methodType));
    }

    public interface MethodHandler {

        class Helper {
            public static final MethodHandle INVOKE;

            static {
                try {
                    INVOKE = MethodHandles.lookup().findVirtual(MethodHandler.class, "invoke",
                            MethodType.methodType(Object.class, SegmentAllocator.class, Object[].class));
                } catch (NoSuchMethodException | IllegalAccessException ex) {
                    // should never happen
                    throw new NativeException("Failed to find method handle", ex);
                }
            }
        }

        Object invoke(SegmentAllocator allocator, Object[] args);

    }

    private abstract static class MethodHandlerBase implements MethodHandler {

        protected final MemorySegment segment;
        protected final FunctionDescriptor functionDescriptor;
        protected final Class<?> retType;

        protected MethodHandlerBase(MemorySegment segment, FunctionDescriptor functionDescriptor, Class<?> retType) {
            this.segment = segment;
            this.functionDescriptor = functionDescriptor;
            this.retType = retType;
        }

        protected Object[] transformArgs(SegmentAllocator allocator, Object[] args) {
            return args;
        }

        protected abstract void preProcess(SegmentAllocator allocator, Object[] args);

        protected MethodHandle createHandle(SegmentAllocator allocator, Object[] args) {
            return Linker.nativeLinker().downcallHandle(this.segment, this.functionDescriptor);
        }

        protected abstract void postProcess(Object[] args, Object[] transformed);

        protected Object processReturn(Object retVal) {
            return retVal;
        }

        @Override
        public final Object invoke(SegmentAllocator allocator, Object[] args) {
            final Object[] transformed = this.transformArgs(allocator, args);
            this.preProcess(allocator, transformed);
            final Object retVal;
            try {
                final MethodHandle haaa = this.createHandle(allocator, transformed);
                retVal = haaa.invokeWithArguments(transformed);
            } catch (Throwable ex) {
                throw new NativeException("Failed to invoke native method", ex);
            }
            this.postProcess(args, transformed);
            return this.processReturn(retVal);
        }

    }

    private static class AutoConverterHandler extends MethodHandlerBase {

        protected AutoConverterHandler(MemorySegment segment, FunctionDescriptor functionDescriptor, Class<?> retType) {
            super(segment, functionDescriptor, retType);
        }

        @Override
        protected Object[] transformArgs(SegmentAllocator allocator, Object[] args) {
            return ConversionUtils.convertArgs(args);
        }

        @Override
        public void preProcess(SegmentAllocator allocator, Object[] args) {
            for (final Object arg : args) {
                if (arg instanceof AutoWritable autoWritable) {
                    autoWritable.autoWrite(true);
                }
            }
        }

        @Override
        public void postProcess(Object[] args, Object[] transformed) {
            for (final Object arg : args) {
                if (arg instanceof AutoReadable autoReadable) {
                    autoReadable.autoRead(false);
                }
            }
        }

        @Override
        protected Object processReturn(Object retVal) {
            return ConversionUtils.convertReturnValue(this.retType, retVal);
        }

    }

    private static final class VariadicHandler extends AutoConverterHandler {

        private FunctionDescriptor fullDescriptor;

        public VariadicHandler(MemorySegment segment, FunctionDescriptor functionDescriptor, Class<?> retType) {
            super(segment, functionDescriptor, retType);
        }

        @Override
        protected Object[] transformArgs(SegmentAllocator allocator, Object[] args) {
            // TODO this can be done more efficiently
            this.fullDescriptor = super.functionDescriptor;
            final Object[] varargs = (Object[]) args[args.length - 1];
            final Object[] fullArgs = new Object[args.length - 1 + varargs.length];
            System.arraycopy(args, 0, fullArgs, 0, args.length - 1);
            final Object[] convertedArgs = super.transformArgs(allocator, fullArgs);

            for (int i = 0; i < varargs.length; i++) {
                final Object vararg = varargs[i];
                final Class<?> varargType = vararg.getClass();
                if (!ConversionUtils.isTypeSupported(varargType)) {
                    throw new NativeException("Parameter type " + varargType.getCanonicalName() + " is unsupported");
                }
                MemoryLayout layout = ConversionUtils.createMemoryLayout(varargType);
                // primitive type promotion is necessary
                if (layout instanceof ValueLayout valueLayout) {
                    final Class<?> carrier = valueLayout.carrier();
                    if (carrier == byte.class || carrier == char.class || carrier == short.class || carrier == int.class) {
                        layout = ValueLayout.JAVA_LONG;
                    } else if (carrier == float.class) {
                        layout = ValueLayout.JAVA_DOUBLE;
                    }
                }
                this.fullDescriptor = this.fullDescriptor.appendArgumentLayouts(layout);
                varargs[i] = ConversionUtils.convertArg(vararg);
            }
            System.arraycopy(convertedArgs, 0, fullArgs, 0, convertedArgs.length);
            System.arraycopy(varargs, 0, fullArgs, args.length - 1, varargs.length);
            return fullArgs;
        }

        @Override
        protected MethodHandle createHandle(SegmentAllocator allocator, Object[] args) {
            return Linker.nativeLinker().downcallHandle(super.segment, this.fullDescriptor);
        }

    }

}
