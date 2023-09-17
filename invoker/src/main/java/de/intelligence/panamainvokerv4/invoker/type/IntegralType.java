package de.intelligence.panamainvokerv4.invoker.type;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.ValueLayout;

import de.intelligence.panamainvokerv4.invoker.reflection.ReflectionUtils;

public class IntegralType extends Number implements NativeType {

    private Number number;

    public IntegralType() {
        super();
    }

    public IntegralType(Number number) {
        this.number = number;
    }

    @Override
    public int intValue() {
        return this.number.intValue();
    }

    @Override
    public long longValue() {
        return this.number.longValue();
    }

    @Override
    public float floatValue() {
        return this.number.floatValue();
    }

    @Override
    public double doubleValue() {
        return this.number.doubleValue();
    }

    @Override
    public Object toNative() {
        return this.number;
    }

    @Override
    public Object toJava(Object nativeObj) {
        final IntegralType instance = ReflectionUtils.newInstance(getClass());
        instance.setNumber(((Number) nativeObj).longValue());
        return instance;
    }

    @Override
    public MemoryLayout getLayout() {
        return ValueLayout.JAVA_INT;
    }

    public void setNumber(long value) {
        this.number = value;
    }

    public Number getNumber() {
        return this.number;
    }

    @Override
    public String toString() {
        return number.toString();
    }

}
