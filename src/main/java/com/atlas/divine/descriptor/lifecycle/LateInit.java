package com.atlas.divine.descriptor.lifecycle;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Represents a descriptive annotation that indicates that the target field or parameter is initialized
 * post-construction.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
public @interface LateInit {
    /**
     * Indicate, whether the target field or parameter is lazily initialized.
     *
     * @return {@code true} if the target field or parameter is lazily initialized, {@code false} otherwise
     */
    boolean lazy() default false;
}
