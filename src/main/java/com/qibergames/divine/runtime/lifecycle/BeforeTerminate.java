package com.qibergames.divine.runtime.lifecycle;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Represents an annotation, that indicates, that the target method should be called before it is terminated and
 * removed from the container.
 * <p>
 * This feature can be used to clean up resources, without having to define and call a special termination method.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface BeforeTerminate {
}
