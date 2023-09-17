package de.intelligence.panamainvokerv4.invoker.alloc;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.lang.reflect.Field;

import sun.misc.Unsafe;

import de.intelligence.panamainvokerv4.invoker.exception.NativeException;

public class NativeSegmentAllocator implements SegmentAllocator {

    private static final Unsafe UNSAFE;

    static {
        try {
            final Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            UNSAFE = (Unsafe) theUnsafe.get(null);
        } catch (ReflectiveOperationException ex) {
            throw new NativeException("Unable to get Unsafe instance", ex);
        }
    }

    @Override
    public MemorySegment allocate(long byteSize, long byteAlignment) {
        if (byteSize < 0 || byteAlignment < 1) {
            throw new IllegalArgumentException("byteSize must be non-negative and byteAlignment must be positive");
        }
        final long address = UNSAFE.allocateMemory(byteSize);
        return MemorySegment.ofAddress(address, byteSize);
    }

}
