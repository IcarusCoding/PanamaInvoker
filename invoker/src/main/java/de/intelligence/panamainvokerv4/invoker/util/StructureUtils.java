package de.intelligence.panamainvokerv4.invoker.util;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.StructLayout;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import de.intelligence.panamainvokerv4.invoker.Panama;
import de.intelligence.panamainvokerv4.invoker.annotation.NativeStruct;
import de.intelligence.panamainvokerv4.invoker.exception.NativeException;
import de.intelligence.panamainvokerv4.invoker.type.IStructure;
import de.intelligence.panamainvokerv4.invoker.type.Structure;

public final class StructureUtils {

    private StructureUtils() {}

    public static boolean isByReference(Class<?> clazz) {
        return !clazz.isAnnotationPresent(Structure.ByValue.class);
    }

    public static boolean isValidStruct(Class<?> type) {
        return type.isAnnotationPresent(NativeStruct.class);
    }

    public static Structure.StructureInfo createStructInfo(Class<? extends IStructure> structClass) {
        // 1. calculate memory layout
        final StructLayout layout = ConversionUtils.convertStruct(structClass);
        // 2. create var handles
        final Map<Field, Structure.FieldInfo> fieldInfos = new HashMap<>();
        Arrays.stream(structClass.getDeclaredFields())
                .forEach(f -> {
                    try {
                        f.setAccessible(true);
                        final Structure.FieldInfo fieldInfo = new Structure.FieldInfo(f,
                                layout.varHandle(MemoryLayout.PathElement.groupElement(f.getName())),
                                MethodHandles.privateLookupIn(structClass, MethodHandles.lookup())
                                        .unreflectVarHandle(f), Panama.getConverters().getConverterInstance(f.getType()));
                        fieldInfos.put(f, fieldInfo);
                    } catch (IllegalAccessException ex) {
                        throw new NativeException("Failed to get var handle for field: " + f, ex);
                    }
                });
        return new Structure.StructureInfo(layout, fieldInfos);
    }

}
