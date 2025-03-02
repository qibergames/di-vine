package com.qibergames.divine.tree.cache;

import com.qibergames.divine.descriptor.generic.Service;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a hook that can be registered in the dependency injection container to modify the dependency instances.
 */
@FunctionalInterface
public interface ContainerHook {
    /**
     * Called when a dependency instance is being created. The hook should return the modified instance.
     *
     * @param dependency the dependency instance that is being created
     * @param descriptor the service descriptor of the dependency
     * @return the modified dependency instance
     */
    @NotNull Object onDependencyCreated(@NotNull Object dependency, @NotNull Service descriptor);
}
