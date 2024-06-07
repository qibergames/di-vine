package com.atlas.divine.tree;

import com.google.common.collect.MapMaker;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Function;

/**
 * Represents a class that provides container instances for specific contexts.
 * <p>
 * The contexts are determined by the {@link #contextKeyMapper} function, which uses the class that calls the container,
 * as the key to determine the context.
 * <p>
 * The container provider is used to group multiple classes together and cache a shared container for each group.
 */
@RequiredArgsConstructor
public abstract class ContainerProvider {
    /**
     * The map of containers that should be used for the specified contexts. The key of a context is resolved by the
     * class that calls the container provider, using the {@link #contextKeyMapper} function.
     */
    private final @NotNull Map<@NotNull Object, @Nullable ContainerRegistry> contextContainers = new MapMaker()
        .weakKeys()
        .weakValues()
        .concurrencyLevel(4)
        .makeMap();

    /**
     * Get the global container to be used for dependency resolving, when the context is not specified explicitly, or
     * the context does not have a container associated with it.
     */
    public abstract @NotNull ContainerRegistry globalContainer();

    /**
     * Get the function that is used to determine what information should be used from the class to group
     * multiple classes together, and cache a shared container for each.
     * <p>
     * By default, classes are associated with their class loaders. This way we can have separate containers for
     * different plugin-like systems.
     */
    public abstract @NotNull Function<@NotNull Class<?>, @Nullable Object> contextKeyMapper();

    /**
     * Get the function that resolves a container for the specified key. The key is resolved from the class by the
     * {@link #contextKeyMapper} function.
     * <p>
     * By default, the global container is returned for all contexts.
     */
    public abstract @NotNull Function<@NotNull Object, @Nullable ContainerRegistry> contextContainerMapper();

    /**
     * Resolve a container instance for the specified context.
     *
     * @param context The context to resolve the container for.
     * @return The container instance for the specified context.
     */
    public @NotNull ContainerRegistry resolveContainer(@NotNull Class<?> context) {
        Object key = contextKeyMapper().apply(context);
        if (key == null)
            return globalContainer();
        ContainerRegistry container = contextContainers.computeIfAbsent(key, contextContainerMapper());
        return container != null ? container : globalContainer();
    }

    /**
     * Register a container instance for the specified context.
     *
     * @param key the key of the context to register the container for
     * @param container the container instance to register
     */
    public void registerContainer(@NotNull Object key, @NotNull ContainerRegistry container) {
        contextContainers.put(key, container);
    }
}
