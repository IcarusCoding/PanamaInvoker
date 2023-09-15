package de.intelligence.panamainvokerv4.invoker.util;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.StructLayout;
import java.lang.foreign.ValueLayout;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import de.intelligence.panamainvokerv4.invoker.Panama;
import de.intelligence.panamainvokerv4.invoker.annotation.FieldOrder;
import de.intelligence.panamainvokerv4.invoker.annotation.NativeStruct;
import de.intelligence.panamainvokerv4.invoker.convert.TypeConverter;
import de.intelligence.panamainvokerv4.invoker.exception.NativeException;
import de.intelligence.panamainvokerv4.invoker.type.NativeType;

public final class ConversionUtils {

    private static final Map<Class<?>, StructLayout> STRUCTS;

    static {
        STRUCTS = new HashMap<>();
    }

    private ConversionUtils() {
    }

    public static void isTypeSupportedOrThrow(Class<?> clazz) {
        if (!isTypeSupported(clazz)) {
            throw new NativeException("Java type " + clazz.getCanonicalName() + " is unsupported");
        }
    }

    public static boolean isTypeSupported(Class<?> clazz) {
        return isPrimitiveOrBoxedPrimitive(clazz) || isNativeType(clazz)
                || Panama.getConverters().getConverterInstance(clazz) != null || clazz.equals(Object.class);
    }

    public static StructLayout convertStruct(Class<?> struct) {
        // 1. check if is valid struct
        if (!isValidStruct(struct)) {
            throw new NativeException("Specified class is not a struct: " + struct.getCanonicalName());
        }

        synchronized (STRUCTS) {
            if (STRUCTS.containsKey(struct)) {
                return STRUCTS.get(struct);
            }
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
        final StructLayout structLayout = MemoryLayout.structLayout(declaredFields.stream()
                .map(f -> createMemoryLayout(f.getType()).withName(f.getName()))
                .toArray(MemoryLayout[]::new));
        synchronized (STRUCTS) {
            return STRUCTS.computeIfAbsent(struct, s -> structLayout);
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

    public static MemoryLayout createMemoryLayout(Class<?> carrier) {
        if (isPrimitiveOrBoxedPrimitive(carrier)) {
            return createForPrimitiveOrWrapper(carrier);
        }
        if (isPrimitiveArray(carrier)) {
            return ValueLayout.ADDRESS;
        }
        final TypeConverter converter = Panama.getConverters().getConverterInstance(carrier);
        if (converter != null) {
            return converter.getLayout();
        }
        throw new NativeException("Failed to convert carrier to native layout representation: " + carrier.getCanonicalName());
    }

    public static Object convertArg(Object arg) {
        if (arg == null) {
            return null;
        }
        final Class<?> argType = arg.getClass();
        final TypeConverter converter = Panama.getConverters().getConverterInstance(argType);
        if (converter != null) {
            return converter.toNative(arg);
        } else if (!ConversionUtils.isPrimitiveOrBoxedPrimitive(argType)) {
            throw new NativeException("Cannot convert java type " + argType.getCanonicalName() + " to native type");
        }
        return arg;
    }

    public static Object[] convertArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return new Object[]{};
        }
        final Object[] copy = new Object[args.length];
        System.arraycopy(args, 0, copy, 0, args.length);
        for (int i = 0; i < copy.length; i++) {
            copy[i] = convertArg(copy[i]);
        }
        return copy;
    }

    public static Object convertReturnValue(Class<?> retType, Object retVal) {
        final TypeConverter converter = Panama.getConverters().getConverterInstance(retType);
        if (converter != null) {
            return converter.toJava(retVal);
        }
        //TODO
        return retVal;
    }

    public static boolean isPrimitiveArray(Class<?> carrier) {
        return carrier.isArray() && carrier.getComponentType().isPrimitive();
    }

    public static MemoryLayout createForPrimitiveOrWrapper(Class<?> carrier) {
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

}
