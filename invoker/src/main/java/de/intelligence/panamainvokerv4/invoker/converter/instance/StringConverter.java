package de.intelligence.panamainvokerv4.invoker.converter.instance;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

import de.intelligence.panamainvokerv4.invoker.Panama;
import de.intelligence.panamainvokerv4.invoker.annotation.Converter;
import de.intelligence.panamainvokerv4.invoker.converter.ITypeConverter;
import de.intelligence.panamainvokerv4.invoker.converter.context.TypeConstructionContext;

@Converter(String.class)
public class StringConverter implements ITypeConverter {

    @Override
    public Object toNative(Object javaObj, TypeConstructionContext context) {
        return Panama.getNativeAllocator().allocateUtf8String((String) javaObj);
    }

    @Override
    public Object toJava(Object nativeObj, TypeConstructionContext context) {
        return MemorySegment.ofAddress((long) nativeObj).getUtf8String(0);
    }

    @Override
    public MemoryLayout getLayout(Class<?> clazz) {
        return ValueLayout.ADDRESS;
    }

}
