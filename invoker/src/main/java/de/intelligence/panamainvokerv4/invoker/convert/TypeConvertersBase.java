package de.intelligence.panamainvokerv4.invoker.convert;

import java.util.HashMap;
import java.util.Map;

import de.intelligence.panamainvokerv4.invoker.reflection.ReflectionUtils;
import de.intelligence.panamainvokerv4.invoker.type.NativeType;

public abstract class TypeConvertersBase implements ITypeConverters {

    private final Map<Class<?>, TypeConverter> typeConverters;

    protected TypeConvertersBase() {
        this.typeConverters = new HashMap<>();
    }

    @Override
    @SuppressWarnings("unchecked")
    public TypeConverter getConverterInstance(Class<?> type) {
        if (!this.typeConverters.containsKey(type)) {
            if (!NativeType.class.isAssignableFrom(type)) {
                return null;
            }
            final TypeConverter converter = new NativeTypeConverter((Class<NativeType>) type);
            this.typeConverters.put(type, converter);
            return converter;
        }
        return this.typeConverters.get(type);
    }

    protected final void addConverter(Class<?> type, TypeConverter converter) {
        this.typeConverters.computeIfAbsent(type, t -> converter);
    }

    public static final class NativeTypeConverter implements TypeConverter {


        private final Class<?> type;
        private final NativeType instance;

        public <T extends NativeType> NativeTypeConverter(Class<T> type) {
            this.type = type;
            this.instance = ReflectionUtils.newInstance(type);
        }

        @Override
        public Object toNative(Object javaObj) {
            return ((NativeType) javaObj).toNative();
        }

        @Override
        public Object toJava(Object nativeObj) {
            return this.instance.toJava(nativeObj);
        }

    }

}
