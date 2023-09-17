package de.intelligence.panamainvokerv4.invoker;

import java.lang.foreign.SegmentAllocator;
import java.lang.foreign.SegmentScope;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.intelligence.panamainvokerv4.invoker.annotation.FieldOrder;
import de.intelligence.panamainvokerv4.invoker.annotation.NativeInterface;
import de.intelligence.panamainvokerv4.invoker.annotation.NativeStruct;
import de.intelligence.panamainvokerv4.invoker.exception.NativeException;
import de.intelligence.panamainvokerv4.invoker.type.IntegralType;
import de.intelligence.panamainvokerv4.invoker.type.Pointer;
import de.intelligence.panamainvokerv4.invoker.type.Structure;
import de.intelligence.panamainvokerv4.invoker.update.UpdatePolicy;

import static org.junit.jupiter.api.Assertions.*;

class NativeInterfaceTests {

    @NativeInterface("invalid")
    interface TestInvalidLibrary {
    }

    interface TestInvalidInterface {
    }

    @NativeInterface("c")
    interface TestStdlib {

        void memcpy(int[] dest, int[] src, int len);

        int puts(String s);

        int puts(Pointer p);

        int printf(String format, Object... args);

    }

    @NativeStruct
    @Structure.ByReference
    static class Point extends Structure {

        @FieldOrder(0)
        int x;
        @FieldOrder(1)
        int y;

    }

    @NativeInterface("User32")
    interface TestUser32 {

        boolean GetCursorPos(Point point);

    }

    private SegmentAllocator allocator;

    @BeforeEach
    void prepare() {
        this.allocator = SegmentAllocator.nativeAllocator(SegmentScope.auto());
    }

    @Test
    void testInvalidInterface() {
        assertThrows(IllegalArgumentException.class, () -> Panama.load(TestInvalidLibrary.class));
        assertThrows(NativeException.class, () -> Panama.load(TestInvalidInterface.class));
    }

    @Test
    void testWrongLoading() {
        assertThrows(NativeException.class, Panama::load);
    }

    @Test
    void testArrayTypes() {
        final TestStdlib stdlib = Panama.load(TestStdlib.class);

        final int[] src = {1, 2, 3};
        final int[] dest = new int[src.length];

        stdlib.memcpy(dest, src, src.length * 4);

        assertArrayEquals(src, dest);
    }

    @Test
    void testStringAndPointerConversion() {
        final TestStdlib stdlib = Panama.load(TestStdlib.class);

        final String testMsg = "Test message";
        final Pointer ptr = new Pointer(this.allocator.allocateUtf8String(testMsg));
        assertTrue(stdlib.puts(testMsg) >= 0);
        assertTrue(stdlib.puts(ptr) >= 0);
    }

    @Test
    void testVarargs() {
        final TestStdlib stdlib = Panama.load(TestStdlib.class);

        assertEquals(10, stdlib.printf("Test %d, %s", new IntegralType(42), "T"));
    }

    @Test
    void testStruct() {
        final TestUser32 user32 = Panama.load(TestUser32.class);

        final Point point = new Point();
        point.x = Integer.MIN_VALUE;
        point.y = Integer.MAX_VALUE;
        point.setReadPolicy(UpdatePolicy.ALWAYS);

        assertTrue(user32.GetCursorPos(point));
        assertNotEquals(Integer.MIN_VALUE, point.x);
        assertNotEquals(Integer.MAX_VALUE, point.y);
    }

}
