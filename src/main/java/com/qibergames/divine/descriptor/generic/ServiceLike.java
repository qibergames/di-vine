package com.qibergames.divine.descriptor.generic;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Represents an annotation, that indicates, that the target class could be treated as a service.
 * <p>
 * This means that the framework will implicitly register the class as a service, if it is annotated
 * with the {@link Service} annotation.
 *
 * @see Service
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.TYPE_USE })
public @interface ServiceLike {
}
