package de.intelligence.panamainvokerv4.invoker.convert;

import java.lang.foreign.MemoryLayout;

public interface TypeConverter {

    Object toNative(Object javaObj);

    Object toJava(Object nativeObj);

    MemoryLayout getLayout();

}
