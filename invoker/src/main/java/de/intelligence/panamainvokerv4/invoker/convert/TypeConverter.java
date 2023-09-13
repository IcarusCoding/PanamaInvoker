package de.intelligence.panamainvokerv4.invoker.convert;

public interface TypeConverter {

    Object toNative(Object javaObj);

    Object toJava(Object nativeObj);

}
