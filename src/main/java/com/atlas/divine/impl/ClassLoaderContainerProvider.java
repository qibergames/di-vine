package com.atlas.divine.impl;

import com.atlas.divine.tree.ContainerProvider;
import com.atlas.divine.tree.ContainerRegistry;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * Represents a class that provides container instances for specific contexts.
 * <p>
 * The contexts are determined by the {@link #contextKeyMapper} function, which uses the class that calls the container,
 * as the key to determine the context.
 * <p>
 * The container provider is used to group multiple classes together and cache a shared container for each group.
 */
@Accessors(fluent = true)
@Getter
public class ClassLoaderContainerProvider extends ContainerProvider {
    /**
     * The global container to be used for dependency resolving, when the context is not specified explicitly, or the
     * context does not have a container associated with it.
     */
    private final @NotNull ContainerRegistry globalContainer = new DefaultContainerImpl(
        null, "global-container"
    );

    /**
     * The function that is used to determine what information should be used from the class to group
     * multiple classes together, and cache a shared container for each.
     * <p>
     * By default, classes are associated with their class loaders. This way we can have separate containers for
     * different plugin-like systems.
     */
    private final @NotNull Function<@NotNull Class<?>, @Nullable Object> contextKeyMapper = Class::getClassLoader;

    /**
     * The function that resolves a container for the specified key. The key is resolved from the class by the
     * {@link #contextKeyMapper} function.
     * <p>
     * By default, the global container is returned for all contexts.
     */
    private final @NotNull Function<@NotNull Object, @Nullable ContainerRegistry> contextContainerMapper = key ->
        new DefaultContainerImpl(globalContainer, "context-container-" + key);
}
