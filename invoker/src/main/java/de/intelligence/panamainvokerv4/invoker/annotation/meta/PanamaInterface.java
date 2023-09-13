package de.intelligence.panamainvokerv4.invoker.annotation.meta;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import de.intelligence.panamainvokerv4.invoker.proxy.IProxyManager;

@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface PanamaInterface {

    Class<? extends IProxyManager> value();

}
