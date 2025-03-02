package com.qibergames.divine.runtime.lifecycle;

import com.qibergames.divine.descriptor.generic.Inject;

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
    /**
     * Retrieve the indication, whether the lifecycle should be called, after the whole dependency tree is resolved.
     * <p>
     * Using this feature, you can ensure, that all lazy initialized fields are injected, when the lifecycle is called.
     * <p>
     * Check out {@link Inject#lazy()} for more information.
     *
     * @return true, if the field should be lazily injected, false otherwise
     */
    boolean lazy() default false;
}
