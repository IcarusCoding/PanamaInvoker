package de.intelligence.panamainvokerv4.invoker.alloc;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.nio.ByteBuffer;

public final class HeapSegmentAllocator implements SegmentAllocator {

    @Override
    public MemorySegment allocate(long byteSize, long byteAlignment) {
        if (byteSize < 0 || byteAlignment < 1) {
            throw new IllegalArgumentException("byteSize must be non-negative and byteAlignment must be positive");
        }
        final ByteBuffer buffer = ByteBuffer.allocate((int) byteSize);
        return MemorySegment.ofBuffer(buffer);
    }

}
