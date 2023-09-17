package de.intelligence.panamainvokerv4.invoker.converter.instance;

import java.lang.foreign.MemoryLayout;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

import de.intelligence.panamainvokerv4.invoker.converter.ITypeConverter;
import de.intelligence.panamainvokerv4.invoker.reflection.ReflectionUtils;
import de.intelligence.panamainvokerv4.invoker.type.NativeType;
import de.intelligence.panamainvokerv4.invoker.annotation.Converter;
import de.intelligence.panamainvokerv4.invoker.converter.ITypeConverterRegistry;
import de.intelligence.panamainvokerv4.invoker.exception.ConversionException;
import de.intelligence.panamainvokerv4.invoker.converter.context.TypeConstructionContext;

public abstract class TypeConverterRegistryBase implements ITypeConverterRegistry {
    
    private final Map<Class<?>, ITypeConverter> classToTypeConverter;
    private final ReentrantLock lock;

    protected TypeConverterRegistryBase() {
        this.classToTypeConverter = new HashMap<>();
        this.lock = new ReentrantLock();
    }

    @Override
    public final <T extends ITypeConverter> void registerConverter(T converter) {
        Objects.requireNonNull(converter);
        final Class<? extends ITypeConverter> converterClass = converter.getClass();
        if (!converterClass.isAnnotationPresent(Converter.class)) {
            throw new ConversionException("Tried to register invalid converter: " + converterClass.getCanonicalName());
        }
        final Class<?>[] supportedTypes = converterClass.getAnnotation(Converter.class).value();
        this.lock.lock();
        try {
            for (final Class<?> supportedType : supportedTypes) {
                if (this.classToTypeConverter.containsKey(supportedType)) {
                    throw new ConversionException("Tried to register an already present conversion type: " + supportedType.getCanonicalName());
                }
                this.classToTypeConverter.put(supportedType, converter);
            }
        } finally {
            this.lock.unlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean isConverterAvailable(Class<?> type) {
        this.lock.lock();
        try {
            if (NativeType.class.isAssignableFrom(type)) {
                this.classToTypeConverter.put(type, new NativeTypeConverter((Class<NativeType>) type));
                return true;
            }
            return this.classToTypeConverter.containsKey(type);
        } finally {
            this.lock.unlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public final ITypeConverter getConverterInstance(Class<?> type) {
        this.lock.lock();
        try {
            if (!this.classToTypeConverter.containsKey(type)) {
                if (NativeType.class.isAssignableFrom(type)) {
                    final ITypeConverter converter = new NativeTypeConverter((Class<NativeType>) type);
                    this.classToTypeConverter.put(type, converter);
                    return converter;
                }
                throw new ConversionException("No type converter found for " + type.getCanonicalName());
            }
            return this.classToTypeConverter.get(type);
        } finally {
            this.lock.unlock();
        }
    }

    @Override
    public Object toJava(Object nativeType, TypeConstructionContext context) {
        return this.getConverterInstance(context.getOriginalType()).toJava(nativeType, context);
    }

    @Override
    public Object toNative(Object javaObj, TypeConstructionContext context) {
        return this.getConverterInstance(javaObj.getClass()).toNative(javaObj, context);
    }

    @Override
    public MemoryLayout getNativeMemoryLayout(Class<?> clazz) {
        return this.getConverterInstance(clazz).getLayout(clazz);
    }

    public static final class NativeTypeConverter implements ITypeConverter {

        private final NativeType instance;

        public <T extends NativeType> NativeTypeConverter(Class<T> type) {
            this.instance = ReflectionUtils.newInstanceUnsafe(type);
        }

        @Override
        public Object toNative(Object javaObj, TypeConstructionContext context) {
            return ((NativeType) javaObj).toNative();
        }

        @Override
        public Object toJava(Object nativeObj, TypeConstructionContext context) {
            return this.instance.toJava(nativeObj);
        }

        @Override
        public MemoryLayout getLayout(Class<?> clazz) {
            return this.instance.getLayout();
        }

    }

}
