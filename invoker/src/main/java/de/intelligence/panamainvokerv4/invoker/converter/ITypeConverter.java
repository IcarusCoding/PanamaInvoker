package de.intelligence.panamainvokerv4.invoker.converter;

import java.lang.foreign.MemoryLayout;

import de.intelligence.panamainvokerv4.invoker.converter.context.TypeConstructionContext;

public interface ITypeConverter {

    Object toNative(Object javaObj, TypeConstructionContext context);

    Object toJava(Object nativeObj, TypeConstructionContext context);

    MemoryLayout getLayout(Class<?> javaClass);

}
