package com.atlas.divine.provider;

import com.atlas.divine.tree.ContainerInstance;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a functional interface that provides an implementation for the specified interface.
 * <p>
 * The {@link #provide(Class, ContainerInstance)} method is called, to resolve the implementation of a service,
 * whenever its corresponding custom annotation is present on a field or constructor parameter.
 *
 * @param <TImplementation>
 */
@FunctionalInterface
public interface AnnotationProvider<TImplementation> {
    /**
     * Provide an implementation for the specified interface.
     *
     * @param target the target interface that the implementation is being provided for
     * @param container the container instance that the implementation is being provided from
     * @return the implementation of the specified interface
     */
    @NotNull TImplementation provide(@NotNull Class<?> target, @NotNull ContainerInstance container);
}
