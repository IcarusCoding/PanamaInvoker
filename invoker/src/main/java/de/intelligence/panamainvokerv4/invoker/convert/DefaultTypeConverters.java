package de.intelligence.panamainvokerv4.invoker.convert;


import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.lang.foreign.SegmentScope;
import java.lang.foreign.ValueLayout;

public class DefaultTypeConverters extends TypeConvertersBase {

    public DefaultTypeConverters() {
        super();
        super.addConverter(String.class, new StringConverter());
    }

    public static final class StringConverter implements TypeConverter {


        @Override
        public Object toNative(Object javaObj) {
            //TODO I dont like this
            return SegmentAllocator.nativeAllocator(SegmentScope.auto()).allocateUtf8String((String) javaObj);
        }

        @Override
        public Object toJava(Object nativeObj) {
            //TODO I dont like this
            return MemorySegment.ofAddress((long) nativeObj).getUtf8String(0);
        }

        @Override
        public MemoryLayout getLayout() {
            return ValueLayout.ADDRESS;
        }

    }

}
