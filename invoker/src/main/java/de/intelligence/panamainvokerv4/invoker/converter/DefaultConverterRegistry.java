package de.intelligence.panamainvokerv4.invoker.converter;

import de.intelligence.panamainvokerv4.invoker.converter.instance.TypeConverterRegistryBase;
import de.intelligence.panamainvokerv4.invoker.converter.instance.PrimitiveArrayConverter;
import de.intelligence.panamainvokerv4.invoker.converter.instance.PrimitiveConverter;
import de.intelligence.panamainvokerv4.invoker.converter.instance.StringConverter;

public class DefaultConverterRegistry extends TypeConverterRegistryBase {

    public DefaultConverterRegistry() {
        super();
        super.registerConverter(new PrimitiveConverter());
        super.registerConverter(new StringConverter());
        super.registerConverter(new PrimitiveArrayConverter());
    }

}
