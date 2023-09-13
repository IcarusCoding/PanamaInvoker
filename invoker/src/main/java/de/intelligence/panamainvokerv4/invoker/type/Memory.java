package de.intelligence.panamainvokerv4.invoker.type;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentScope;

public class Memory extends Pointer {

    private final long size;

    public Memory(long bytes) {
        this(MemorySegment.allocateNative(bytes, SegmentScope.auto()));
    }

    public Memory(Pointer ptr) {
        this(ptr.getSegment());
    }

    public Memory(MemorySegment segment) {
        super(segment);
        this.size = segment.byteSize();
    }

    public void zero() {
        super.setMem((byte) 0);
    }

}
