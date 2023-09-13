package de.intelligence.panamainvokerv4.invoker.type;

import java.lang.foreign.MemoryLayout;

public interface NativeType {

    Object toNative();

    Object toJava(Object nativeObj);

    MemoryLayout getLayout();

}
