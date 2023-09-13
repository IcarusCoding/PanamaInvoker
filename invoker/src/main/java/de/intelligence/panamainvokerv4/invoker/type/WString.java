package de.intelligence.panamainvokerv4.invoker.type;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.lang.foreign.SegmentScope;
import java.lang.foreign.ValueLayout;
import java.nio.ByteOrder;
import java.nio.CharBuffer;

public final class WString implements CharSequence, Comparable<WString>, NativeType {

    private final String delegate;

    public WString(String delegate) {
        this.delegate = delegate;
    }

    public WString() {
        this.delegate = "";
    }

    @Override
    public int length() {
        return this.delegate.length();
    }

    @Override
    public char charAt(int index) {
        return this.delegate.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return new WString(this.delegate.substring(start, end));
    }

    @Override
    public int compareTo(WString other) {
        return this.delegate.compareTo(other.toString());
    }

    @Override
    public boolean equals(Object obj) {
        return this.delegate.equals(obj);
    }

    @Override
    public int hashCode() {
        return this.delegate.hashCode();
    }

    @Override
    public String toString() {
        return this.delegate;
    }

    @Override
    public Object toNative() {
        //TODO I dont like this
        final MemorySegment segment = SegmentAllocator.nativeAllocator(SegmentScope.auto())
                .allocateArray(ValueLayout.JAVA_CHAR, (long) this.delegate.length() + 1);
        segment.asByteBuffer().order(ByteOrder.nativeOrder()).asCharBuffer().put(this.delegate).put('\0');
        return segment;
    }

    @Override
    public Object toJava(Object nativeObj) {
        //TODO I dont like this
        final CharBuffer buf = ((MemorySegment) nativeObj).asByteBuffer().order(ByteOrder.nativeOrder()).asCharBuffer();
        int lim = 0;
        for(int end = buf.limit(); lim < end && buf.get(lim) != 0; lim++);
        return buf.limit(lim).toString();
    }

    @Override
    public MemoryLayout getLayout() {
        return ValueLayout.ADDRESS;
    }

}
