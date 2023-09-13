package de.intelligence.panamainvokerv4.invoker.util;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.lang.foreign.SegmentScope;
import java.lang.foreign.StructLayout;
import java.lang.foreign.ValueLayout;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import jdk.jfr.MemoryAddress;

import de.intelligence.panamainvokerv4.invoker.annotation.FieldOrder;
import de.intelligence.panamainvokerv4.invoker.annotation.NativeStruct;
import de.intelligence.panamainvokerv4.invoker.exception.NativeException;
import de.intelligence.panamainvokerv4.invoker.reflection.ReflectionUtils;
import de.intelligence.panamainvokerv4.invoker.type.NativeType;
import de.intelligence.panamainvokerv4.invoker.type.Pointer;

public final class MemoryLayoutUtils {

    private MemoryLayoutUtils() {}

    public static FunctionDescriptor createMemoryLayout(Method method) {
        final Class<?>[] parameterTypes = method.getParameterTypes();
        final Class<?> returnType = method.getReturnType();
        final MemoryLayout[] parameterLayouts = new MemoryLayout[parameterTypes.length];
        for (int idx = 0; idx < parameterLayouts.length; idx++) {
            parameterLayouts[idx] = createMemoryLayout(parameterTypes[idx]);
        }
        if (Void.TYPE.equals(returnType)) {
            return FunctionDescriptor.ofVoid(parameterLayouts);
        }
        return FunctionDescriptor.of(createMemoryLayout(returnType), parameterLayouts);
    }

    //TODO this should be cached
    public static MemoryLayout createMemoryLayout(Class<?> carrier) {
        // 1. Test for primitives
        if (isPrimitiveOrBoxedPrimitive(carrier)) {
            return convertPrimitiveOrWrapper(carrier);
        }
        // 2. Test for structure type
        //TODO this should only be used when passing structures by value
        if (isValidStruct(carrier)) {
            //return convertStruct(carrier);
        }

        if (isPrimitiveArray(carrier)) {
            return ValueLayout.ADDRESS;
        }

        if (isNativeType(carrier)) {
            //TODO stupid and very ugly solution -> create converter classes that do not need instances of the actual type maybe
            return ((NativeType) ReflectionUtils.newInstance(carrier)).getLayout();
        }
        if (Pointer.class.isAssignableFrom(carrier) || String.class.isAssignableFrom(carrier) /*|| StructureSupport.class.isAssignableFrom(carrier)*/) {
            return ValueLayout.ADDRESS;
        }
        //TODO we should support parameter passing by value, dunno how yet though
        throw new NativeException("Failed to convert carrier to native representation: " + carrier.getCanonicalName());
    }

    private static boolean isPrimitiveArray(Class<?> carrier) {
        return carrier.isArray() && carrier.getComponentType().isPrimitive();
    }

    public static StructLayout convertStruct(Class<?> struct) {
        // 1. check if is valid struct
        if (!isValidStruct(struct)) {
            throw new NativeException("Specified class is not a struct: " + struct.getCanonicalName());
        }
        // 2. check if ALL or NO fields have the field order annotation
        final List<Field> declaredFields = Arrays.stream(struct.getDeclaredFields()).collect(Collectors.toList());
        boolean atLeastOneAnnotated = declaredFields.stream().anyMatch(f -> f.isAnnotationPresent(FieldOrder.class));
        if (atLeastOneAnnotated) {
            if (declaredFields.stream().anyMatch(f -> !f.isAnnotationPresent(FieldOrder.class))) {
                throw new NativeException("None or all fields have to be annotated with @FieldOrder for struct " + struct.getCanonicalName());
            }
            final List<Integer> orderNums = new ArrayList<>();
            declaredFields.forEach(f -> {
                final int val = f.getAnnotation(FieldOrder.class).value();
                if (orderNums.contains(val)) {
                    throw new NativeException("FieldOrder annotations need distinct order priorities");
                }
                orderNums.add(val);
            });
            declaredFields.sort(Comparator.comparingInt(f -> f.getAnnotation(FieldOrder.class).value()));
        } else {
            // 3. check if fieldOrder is specified in the type annotation
            final NativeStruct nativeStruct = struct.getAnnotation(NativeStruct.class);
            final List<String> fieldOrderElems = Arrays.stream(nativeStruct.fieldOrder()).toList();
            if (!fieldOrderElems.isEmpty()) {
                if (fieldOrderElems.size() != declaredFields.size()) {
                    throw new NativeException("size of fieldOrder elements does not match field count for struct " + struct.getCanonicalName());
                }
                if (declaredFields.stream().anyMatch(f -> !fieldOrderElems.contains(f.getName()))) {
                    throw new NativeException("mismatch between fieldOrder names and field names for struct " + struct.getCanonicalName());
                }
                declaredFields.sort(Comparator.comparingInt(f -> fieldOrderElems.indexOf(f.getName())));
            } else if (!declaredFields.isEmpty()) {
                System.err.println("WARNING: no explicit field order given for struct " + struct.getCanonicalName());
            }
        }

        // 4. create final layout
        return MemoryLayout.structLayout(declaredFields.stream()
                .map(f -> createMemoryLayout(f.getType()).withName(f.getName()))
                .toArray(MemoryLayout[]::new));
    }

    public static MemoryLayout convertPrimitiveOrWrapper(Class<?> carrier) {
        if (carrier == boolean.class || carrier == Boolean.class) {
            return ValueLayout.JAVA_BOOLEAN;
        } else if (carrier == byte.class || carrier == Byte.class) {
            return ValueLayout.JAVA_BYTE;
        } else if (carrier == short.class || carrier == Short.class) {
            return ValueLayout.JAVA_SHORT;
        } else if (carrier == char.class || carrier == Character.class) {
            return ValueLayout.JAVA_CHAR;
        } else if (carrier == int.class || carrier == Integer.class) {
            return ValueLayout.JAVA_INT;
        } else if (carrier == long.class || carrier == Long.class) {
            return ValueLayout.JAVA_LONG;
        } else if (carrier == float.class || carrier == Float.class) {
            return ValueLayout.JAVA_FLOAT;
        } else if (carrier == double.class || carrier == Double.class) {
            return ValueLayout.JAVA_DOUBLE;
        } else {
            throw new NativeException("Carrier is not a java primitive or boxed primitive: " + carrier.getCanonicalName());
        }
    }

    public static boolean isPrimitiveOrBoxedPrimitive(Class<?> type) {
        return type.isPrimitive() || isBoxedPrimitive(type);
    }

    public static boolean isValidStruct(Class<?> type) {
        return type.isAnnotationPresent(NativeStruct.class);
    }

    private static boolean isNativeType(Class<?> carrier) {
        return NativeType.class.isAssignableFrom(carrier);
    }

    public static boolean isBoxedPrimitive(Class<?> type) {
        return type == Integer.class ||
                type == Long.class ||
                type == Short.class ||
                type == Byte.class ||
                type == Character.class ||
                type == Double.class ||
                type == Float.class ||
                type == Boolean.class;
    }

}
