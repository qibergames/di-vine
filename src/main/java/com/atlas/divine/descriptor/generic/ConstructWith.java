package com.atlas.divine.descriptor.generic;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Represents an annotation that tells the container to inject a dependency with a constructor.
 * This annotation is applied to a constructor of a dependency, if the class has multiple constructors.
 * <p>
 * If the class has only one constructor, the container will automatically use that constructor.
 * Otherwise, the container will throw an exception, if no constructor is annotated with this annotation,
 * as the dependency injector does not know which constructor to use.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.CONSTRUCTOR)
public @interface ConstructWith {
}
