package com.qibergames.divine.provider;

import com.qibergames.divine.descriptor.generic.ServiceLike;
import com.qibergames.divine.tree.ContainerInstance;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;

/**
 * Represents a functional interface that provides an implementation for a service interface.
 * <p>
 * The {@link #provide(Class, Annotation, ContainerInstance, InjectionHandle)} method is called, to resolve the
 * implementation of a service, whenever its corresponding custom annotation is present on a field or constructor
 * parameter.
 *
 * @param <TImplementation> the type of the service that this class provides an implementation for
 * @param <TAnnotation> the type of the annotation that is present on the field or constructor parameter
 */
@FunctionalInterface
public interface AnnotationProvider<@ServiceLike TImplementation, TAnnotation extends Annotation> {
    /**
     * Provide an implementation for the specified interface.
     *
     * @param target the target interface that the implementation is being provided for
     * @param annotation the annotation that is present on the field or constructor parameter
     * @param container the container instance that the implementation is being provided from
     * @param handle the type of the injection and information of the requesting service
     *
     * @return the implementation of the specified interface
     */
    @NotNull TImplementation provide(
        @NotNull Class<?> target, @NotNull TAnnotation annotation, @NotNull ContainerInstance container,
        @NotNull InjectionHandle handle
    );
}
