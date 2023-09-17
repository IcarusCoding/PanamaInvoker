package de.intelligence.panamainvokerv4.invoker.converter;

import java.lang.foreign.MemoryLayout;

import de.intelligence.panamainvokerv4.invoker.converter.context.TypeConstructionContext;

public interface ITypeConverterRegistry {

    <T extends ITypeConverter> void registerConverter(T converter);
    ITypeConverter getConverterInstance(Class<?> type);
    boolean isConverterAvailable(Class<?> type);
    Object toJava(Object nativeObj, TypeConstructionContext context);
    Object toNative(Object javaObj, TypeConstructionContext context);
    MemoryLayout getNativeMemoryLayout(Class<?> javaType);

}
