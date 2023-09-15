package de.intelligence.panamainvokerv4.invoker.type;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

public class Pointer implements NativeType {

    public static Pointer NULL_PTR = new Pointer(0);

    protected MemorySegment segment;

    public Pointer() {
        this(MemorySegment.NULL);
    }

    public Pointer(long address) {
        this(MemorySegment.ofAddress(address));
    }

    public Pointer(MemorySegment segment) {
        this.segment = segment;
    }

    public boolean isNullPtr() {
        return this.getAddress() == 0;
    }

    public void setMem(byte val) {
        this.segment.fill(val);
    }

    public long getAddress() {
        return this.segment.address();
    }

    public MemorySegment getSegment() {
        return this.segment;
    }

    public void setPointer(long offset, Pointer pointer) {
        this.segment.set(ValueLayout.ADDRESS, offset, pointer.segment);
    }

    public Pointer getPointer(long offset) {
        return new Pointer(this.segment.get(ValueLayout.ADDRESS, offset));
    }

    @Override
    public Object toNative() {
        return this.segment;
    }

    @Override
    public Object toJava(Object nativeObj) {
        return new Pointer((long) nativeObj);
    }

    @Override
    public MemoryLayout getLayout() {
        return ValueLayout.ADDRESS;
    }

    @Override
    public String toString() {
        return this.segment.toString();
    }

}
