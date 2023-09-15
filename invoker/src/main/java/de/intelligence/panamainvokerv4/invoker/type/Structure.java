package de.intelligence.panamainvokerv4.invoker.type;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentScope;
import java.lang.foreign.StructLayout;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import de.intelligence.panamainvokerv4.invoker.convert.TypeConverter;
import de.intelligence.panamainvokerv4.invoker.exception.NativeException;
import de.intelligence.panamainvokerv4.invoker.update.UpdatePolicy;
import de.intelligence.panamainvokerv4.invoker.util.ConversionUtils;
import de.intelligence.panamainvokerv4.invoker.util.StructureUtils;

public abstract class Structure implements IStructure {

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ByValue {
    }

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ByReference {
    }

    public record FieldInfo(Field field, VarHandle nativeHandle, VarHandle javaHandle, TypeConverter converter) {
    }

    public record StructureInfo(StructLayout layout, Map<Field, FieldInfo> fields) {
    }

    protected static final Map<Class<?>, StructureInfo> LAYOUTS;

    static {
        LAYOUTS = new HashMap<>();
    }

    private final StructureInfo structureInfo;

    private Memory structMem;
    private UpdatePolicy readPolicy;
    private UpdatePolicy writePolicy;

    protected Structure() {
        this(null);
    }

    protected Structure(Pointer structPtr) {
        this.readPolicy = UpdatePolicy.AFTER_USED;
        this.writePolicy = UpdatePolicy.AFTER_USED;
        final Class<? extends IStructure> structureClass = this.getClass();
        synchronized (LAYOUTS) {
            this.structureInfo = LAYOUTS.computeIfAbsent(structureClass, sC -> StructureUtils.createStructInfo(structureClass));
        }
        if (structPtr == null) {
            this.structMem = new Memory(MemorySegment.allocateNative(this.structureInfo.layout, SegmentScope.auto()));
            this.structMem.zero();
        }
    }

    @Override
    public void read() {
        if (this.structMem.isNullPtr()) {
            return;
        }
        for (FieldInfo(
                Field field, VarHandle nativeHandle, VarHandle javaHandle, TypeConverter converter
        ) : this.structureInfo.fields.values()) {
            if (converter != null) {
                javaHandle.set(this, converter.toJava(nativeHandle.get(this.structMem.getSegment())));
                continue;
            }
            if (!ConversionUtils.isPrimitiveOrBoxedPrimitive(field.getType())) {
                throw new NativeException("Cannot convert between java type " + field.getType().getCanonicalName() + " and native type");
            }
            javaHandle.set(this, nativeHandle.get(this.structMem.getSegment()));
        }
    }

    @Override
    public void write() {
        if (this.structMem.isNullPtr()) {
            return;
        }
        for (FieldInfo(
                Field field, VarHandle nativeHandle, VarHandle javaHandle, TypeConverter converter
        ) : this.structureInfo.fields.values()) {
            if (converter != null) {
                nativeHandle.set(this.structMem.getSegment(), converter.toNative(javaHandle.get(this)));
            }
            if (!ConversionUtils.isPrimitiveOrBoxedPrimitive(field.getType())) {
                throw new NativeException("Cannot convert between java type " + field.getType().getCanonicalName() + " and native type");
            }
            nativeHandle.set(this.structMem.getSegment(), javaHandle.get(this));
        }
    }

    @Override
    public long address() {
        return this.structMem.getAddress();
    }

    @Override
    public MemorySegment segment() {
        return this.structMem.getSegment();
    }

    @Override
    public void autoRead(boolean fromUsage) {
        if (this.readPolicy == UpdatePolicy.NEVER) {
            return;
        }
        if (this.readPolicy == UpdatePolicy.ALWAYS || fromUsage) {
            this.read();
        }
    }

    @Override
    public void setReadPolicy(UpdatePolicy readPolicy) {
        this.readPolicy = readPolicy;
    }

    @Override
    public void autoWrite(boolean fromUsage) {
        if (this.writePolicy == UpdatePolicy.NEVER) {
            return;
        }
        if (this.writePolicy == UpdatePolicy.ALWAYS || fromUsage) {
            this.write();
        }
    }

    @Override
    public void setWritePolicy(UpdatePolicy writePolicy) {
        this.writePolicy = writePolicy;
    }

}
