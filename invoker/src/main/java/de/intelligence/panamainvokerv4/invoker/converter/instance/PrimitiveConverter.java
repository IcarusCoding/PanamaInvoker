package de.intelligence.panamainvokerv4.invoker.converter.instance;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.ValueLayout;

import de.intelligence.panamainvokerv4.invoker.annotation.Converter;
import de.intelligence.panamainvokerv4.invoker.converter.ITypeConverter;
import de.intelligence.panamainvokerv4.invoker.exception.ConversionException;
import de.intelligence.panamainvokerv4.invoker.converter.context.TypeConstructionContext;

@Converter({byte.class, Byte.class, char.class, Character.class, short.class, Short.class, int.class, Integer.class,
        long.class, Long.class, float.class, Float.class, double.class, Double.class, boolean.class, Boolean.class})
public class PrimitiveConverter implements ITypeConverter {

    @Override
    public Object toNative(Object javaObj, TypeConstructionContext context) {
        return javaObj;
    }

    @Override
    public Object toJava(Object nativeObj, TypeConstructionContext context) {
        return nativeObj;
    }

    @Override
    public MemoryLayout getLayout(Class<?> clazz) {
        if (!clazz.isPrimitive()) {
            clazz = this.getPrimitive(clazz);
        }
        if (clazz == byte.class) {
            return ValueLayout.JAVA_BYTE;
        }
        if (clazz == char.class) {
            return ValueLayout.JAVA_CHAR;
        }
        if (clazz == short.class) {
            return ValueLayout.JAVA_SHORT;
        }
        if (clazz == int.class) {
            return ValueLayout.JAVA_INT;
        }
        if (clazz == long.class) {
            return ValueLayout.JAVA_LONG;
        }
        if (clazz == float.class) {
            return ValueLayout.JAVA_FLOAT;
        }
        if (clazz == double.class) {
            return ValueLayout.JAVA_DOUBLE;
        }
        if (clazz == boolean.class) {
            return ValueLayout.JAVA_BOOLEAN;
        }
        throw new IllegalArgumentException("Class not supported by this converter: " + clazz.getCanonicalName());
    }

    protected final Class<?> getPrimitive(Class<?> primOrBoxed) {
        if (primOrBoxed == boolean.class || primOrBoxed == Boolean.class) {
            return boolean.class;
        } else if (primOrBoxed == byte.class || primOrBoxed == Byte.class) {
            return byte.class;
        } else if (primOrBoxed == short.class || primOrBoxed == Short.class) {
            return short.class;
        } else if (primOrBoxed == char.class || primOrBoxed == Character.class) {
            return char.class;
        } else if (primOrBoxed == int.class || primOrBoxed == Integer.class) {
            return int.class;
        } else if (primOrBoxed == long.class || primOrBoxed == Long.class) {
            return long.class;
        } else if (primOrBoxed == float.class || primOrBoxed == Float.class) {
            return float.class;
        } else if (primOrBoxed == double.class || primOrBoxed == Double.class) {
            return double.class;
        } else {
            throw new ConversionException("Carrier is not a java primitive or boxed primitive: " + primOrBoxed.getCanonicalName());
        }
    }

}
