package com.qibergames.divine.inspector;

import com.qibergames.divine.tree.ContainerInstance;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;

/**
 * Represents a functional interface that provides access to a method of a service instance.
 * <p>
 * The {@link #inspect(ServiceMethod, Annotation, ContainerInstance)} method is called for each class method, that has
 * a special annotation registered in the container.
 */
public interface MethodInspector<TAnnotation extends Annotation> {
    /**
     * Inspect a class method of a service class, that annotated a method with a special annotation.
     *
     * @param method the method of the service
     * @param annotation the annotation instance
     * @param container the container that requested a dependency for the service
     */
    void inspect(
        @NotNull ServiceMethod method, @NotNull TAnnotation annotation, @NotNull ContainerInstance container
    );
}
