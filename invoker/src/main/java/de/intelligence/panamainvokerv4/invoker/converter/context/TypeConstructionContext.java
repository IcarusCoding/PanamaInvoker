package de.intelligence.panamainvokerv4.invoker.converter.context;

public class TypeConstructionContext {

    private final Class<?> originalType;

    public TypeConstructionContext(Class<?> originalType) {
        this.originalType = originalType;
    }

    public Class<?> getOriginalType() {
        return this.originalType;
    }

}
