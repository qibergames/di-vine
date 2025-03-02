package com.qibergames.divine.provider;

import com.qibergames.divine.descriptor.generic.Inject;
import com.qibergames.divine.tree.ContainerInstance;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a lazy accessor of a dependency value.
 * <p>
 * This interface is used to inject dependencies into a class, without actually retrieving the dependency.
 * The dependency will be retrieved when the {@link #get()} method is called.
 * <p>
 * This feature is useful, when you only want to instantiate a dependency during runtime, after service initialization.
 * <p>
 * Using {@link Ref} can fix circular dependencies, by allowing you to lazily inject a dependency.
 *
 * @param <T> the type of the dependency
 */
public interface Ref<T> {
    /**
     * Retrieve the implementation of the service dependency, when it is requested.
     * <p>
     * This method will implicitly call {@link ContainerInstance#get(Class)} with the specified {@link T} type
     * and the metadata passed by {@link Inject} descriptor.
     *
     * @return the service implementation
     */
    @NotNull T get();
}
