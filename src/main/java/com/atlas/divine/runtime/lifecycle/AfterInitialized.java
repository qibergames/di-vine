package com.atlas.divine.runtime.lifecycle;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Represents an annotation, that indicates, that the annotated method should be called after the service class is
 * initialized. This annotation is useful, if you do not want to define and call a special initialization method.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AfterInitialized {
}
