package de.intelligence.panamainvokerv4.invoker.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import de.intelligence.panamainvokerv4.invoker.annotation.meta.PanamaInterface;
import de.intelligence.panamainvokerv4.invoker.proxy.NativeInterfaceProxyManager;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@PanamaInterface(NativeInterfaceProxyManager.class)
public @interface NativeInterface {

    String value();

}
