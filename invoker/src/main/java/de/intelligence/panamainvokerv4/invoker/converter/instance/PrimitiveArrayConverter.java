package de.intelligence.panamainvokerv4.invoker.converter.instance;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;

import de.intelligence.panamainvokerv4.invoker.Panama;
import de.intelligence.panamainvokerv4.invoker.annotation.Converter;
import de.intelligence.panamainvokerv4.invoker.converter.context.TypeConstructionContext;
import de.intelligence.panamainvokerv4.invoker.exception.ConversionException;

@Converter({int[].class, Integer[].class, byte[].class, Byte[].class, short[].class, Short[].class, long[].class,
        Long[].class, float[].class, Float[].class, double[].class, Double[].class, char[].class, Character[].class})
public class PrimitiveArrayConverter extends PrimitiveConverter {

    private static final Map<Class<?>, MethodHandle> SEG_OF_ARRAY_HANDLES;
    private static final Map<Class<?>, MethodHandle> SEG_TO_ARRAY_HANDLES;

    static {
        SEG_OF_ARRAY_HANDLES = new HashMap<>();
        SEG_TO_ARRAY_HANDLES = new HashMap<>();
        final MethodHandles.Lookup lookup = MethodHandles.lookup();
        try {
            SEG_OF_ARRAY_HANDLES.put(int.class, lookup.findStatic(MemorySegment.class, "ofArray", MethodType.methodType(MemorySegment.class, int[].class)));
            SEG_OF_ARRAY_HANDLES.put(byte.class, lookup.findStatic(MemorySegment.class, "ofArray", MethodType.methodType(MemorySegment.class, byte[].class)));
            SEG_OF_ARRAY_HANDLES.put(short.class, lookup.findStatic(MemorySegment.class, "ofArray", MethodType.methodType(MemorySegment.class, short[].class)));
            SEG_OF_ARRAY_HANDLES.put(long.class, lookup.findStatic(MemorySegment.class, "ofArray", MethodType.methodType(MemorySegment.class, long[].class)));
            SEG_OF_ARRAY_HANDLES.put(float.class, lookup.findStatic(MemorySegment.class, "ofArray", MethodType.methodType(MemorySegment.class, float[].class)));
            SEG_OF_ARRAY_HANDLES.put(double.class, lookup.findStatic(MemorySegment.class, "ofArray", MethodType.methodType(MemorySegment.class, double[].class)));
            SEG_OF_ARRAY_HANDLES.put(char.class, lookup.findStatic(MemorySegment.class, "ofArray", MethodType.methodType(MemorySegment.class, char[].class)));
            SEG_TO_ARRAY_HANDLES.put(int.class, lookup.findVirtual(MemorySegment.class, "toArray", MethodType.methodType(int[].class, ValueLayout.OfInt.class)));
            SEG_TO_ARRAY_HANDLES.put(byte.class, lookup.findVirtual(MemorySegment.class, "toArray", MethodType.methodType(byte[].class, ValueLayout.OfByte.class)));
            SEG_TO_ARRAY_HANDLES.put(short.class, lookup.findVirtual(MemorySegment.class, "toArray", MethodType.methodType(short[].class, ValueLayout.OfShort.class)));
            SEG_TO_ARRAY_HANDLES.put(long.class, lookup.findVirtual(MemorySegment.class, "toArray", MethodType.methodType(long[].class, ValueLayout.OfLong.class)));
            SEG_TO_ARRAY_HANDLES.put(float.class, lookup.findVirtual(MemorySegment.class, "toArray", MethodType.methodType(float[].class, ValueLayout.OfFloat.class)));
            SEG_TO_ARRAY_HANDLES.put(double.class, lookup.findVirtual(MemorySegment.class, "toArray", MethodType.methodType(double[].class, ValueLayout.OfDouble.class)));
            SEG_TO_ARRAY_HANDLES.put(char.class, lookup.findVirtual(MemorySegment.class, "toArray", MethodType.methodType(char[].class, ValueLayout.OfChar.class)));
        } catch (ReflectiveOperationException ex) {
            throw new ConversionException("Failed to find necessary method: ", ex);
        }
    }

    @Override
    public Object toNative(Object javaObj, TypeConstructionContext context) {
        if (javaObj == null) {
            return MemorySegment.NULL;
        }
        Class<?> componentType = javaObj.getClass().getComponentType();
        if (!componentType.isPrimitive()) {
            componentType = super.getPrimitive(componentType);
        }
        final MemoryLayout componentLayout = super.getLayout(componentType);
        final int arrLen = Array.getLength(javaObj);
        final MemorySegment arrAlloc = Panama.getNativeAllocator().allocateArray(componentLayout, arrLen);
        try {
            arrAlloc.copyFrom((MemorySegment) SEG_OF_ARRAY_HANDLES.get(componentType).invoke(javaObj));
        } catch (Throwable ex) {
            throw new ConversionException("Failed to convert array to native: " + javaObj.getClass().getCanonicalName(), ex);
        }
        return arrAlloc;
    }

    @Override
    public Object toJava(Object nativeObj, TypeConstructionContext context) {
        final MemorySegment segment = (MemorySegment) nativeObj;
        final Class<?> componentTypeOriginal = context.getOriginalType().getComponentType();
        Class<?> componentType = componentTypeOriginal;
        if (!componentType.isPrimitive()) {
            componentType = super.getPrimitive(componentType);
        }
        final ValueLayout componentLayout = (ValueLayout) super.getLayout(componentType);
        try {
            final Object javaArray = SEG_TO_ARRAY_HANDLES.get(componentType).invoke(segment, componentLayout);
            final int len = Array.getLength(javaArray);
            //TODO something will die here when an array is to be returned i think
            //TODO try inplace conversion when everything else works
            if (componentTypeOriginal.isPrimitive()) {
                return javaArray;
            } else {
                final Object array = Array.newInstance(componentTypeOriginal, len);
                for (int i = 0; i < len; i++) {
                    Array.set(array, i, Array.get(javaArray, i));
                }
                return array;
            }
        } catch (Throwable ex) {
            throw new ConversionException("Failed to convert native type to array: " + nativeObj, ex);
        }
    }

    @Override
    public MemoryLayout getLayout(Class<?> clazz) {
        return ValueLayout.ADDRESS;
    }

}
