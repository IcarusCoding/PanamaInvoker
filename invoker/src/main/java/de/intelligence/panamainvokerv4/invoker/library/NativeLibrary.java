package de.intelligence.panamainvokerv4.invoker.library;

import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentScope;
import java.lang.foreign.SymbolLookup;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import de.intelligence.panamainvokerv4.invoker.exception.NativeException;

public final class NativeLibrary {

    private final Map<Method, NativeFunction> functions;
    private final String libraryName;
    private final SymbolLookup symbolLookup;

    public NativeLibrary(String libraryName, SegmentScope segmentScope) {
        this.functions = new HashMap<>();
        this.libraryName = libraryName;
        if ("c".equals(libraryName) || "stdlib".equals(libraryName)) {
            this.symbolLookup = Linker.nativeLinker().defaultLookup();
        } else {
            this.symbolLookup = SymbolLookup.libraryLookup(libraryName, segmentScope);
        }
    }

    public void registerMethod(Method method) {
        // TODO maybe auto convert method names and create something more abstract like a FunctionCreator
        this.functions.put(method, new NativeFunction(this.findSymbol(method.getName()), method));
    }

    public MemorySegment findSymbol(String name) {
        return this.symbolLookup.find(name).orElseThrow(() ->
                new NativeException("Failed to find symbol " + name + " in library " + this.libraryName));
    }

    public NativeFunction getFunction(Method method) {
        if (!this.functions.containsKey(method)) {
            throw new NativeException("No native function for method " + method + " found in library " + this.libraryName);
        }
        return this.functions.get(method);
    }

}
