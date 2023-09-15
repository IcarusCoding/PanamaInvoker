package de.intelligence.panamainvokerv4.invoker.type;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

import de.intelligence.panamainvokerv4.invoker.update.AutoUpdatable;
import de.intelligence.panamainvokerv4.invoker.util.ConversionUtils;
import de.intelligence.panamainvokerv4.invoker.util.StructureUtils;

public interface IStructure extends AutoUpdatable, NativeType {

    void read();

    void write();

    long address();

    MemorySegment segment();

    @Override
    default Object toNative() {
        return this.segment();
    }

    @Override
    default MemoryLayout getLayout() {
        final Class<? extends IStructure> structureClass = this.getClass();
        return StructureUtils.isByReference(structureClass) ? ValueLayout.ADDRESS : ConversionUtils.convertStruct(structureClass);
    }

    @Override
    default Object toJava(Object nativeObj) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

}
