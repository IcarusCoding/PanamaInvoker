package de.intelligence.panamainvokerv4.invoker.convert;

public interface ITypeConverters {

    TypeConverter getConverterInstance(Class<?> type);

}
