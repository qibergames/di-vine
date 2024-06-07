package com.atlas.divine.tree;

import com.atlas.divine.tree.cache.ContainerHook;
import com.atlas.divine.exception.UnknownDependencyException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Represents a scope specific container instance that manages dependencies and token values.
 */
public interface ContainerInstance {
    /**
     * Register a hook that will be called when a dependency instance is being created.
     * The hook will be called with the dependency instance as an argument and should return the modified instance.
     *
     * @param id the unique identifier of the hook
     * @param hook the function that will be called when a dependency instance is being created
     */
    void addHook(@NotNull String id, @NotNull ContainerHook hook);

    /**
     * Remove a hook from the container instance.
     *
     * @param id the unique identifier of the hook
     */
    void removeHook(@NotNull String id);

    /**
     * Retrieve an instance from the container for the specified class type. Based on the service descriptor,
     * a dependency instance may be retrieved from the container cache, or a new instance is created.
     *
     * @param type the class type of the dependency
     * @return the instance of the desired dependency type
     * @param <T> the type of the dependency
     *
     * @throws UnknownDependencyException if the dependency is not found, invalid, or the caller context
     * does not have permission to access the dependency
     */
    @NotNull <T> T get(@NotNull Class<T> type);

    /**
     * Retrieve an instance from the container for the specified class type. Based on the service descriptor,
     * a dependency instance may be retrieved from the container cache, or a new instance is created.
     *
     * @param type the class type of the dependency
     * @param context the caller class that the container is being called from
     * @return the instance of the desired dependency type
     * @param <T> the type of the dependency
     *
     * @throws UnknownDependencyException if the dependency is not found, invalid, or the caller context
     * does not have permission to access the dependency
     */
    @NotNull <T> T get(@NotNull Class<T> type, @NotNull Class<?> context);

    /**
     * Retrieve an instance from the container for the specified class type. Based on the service descriptor,
     * a dependency instance may be retrieved from the container cache, or a new instance is created.
     *
     * @param type the class type of the dependency
     * @param properties the properties to create the instance with
     * @return the instance of the desired dependency type
     *
     * @param <TService> the type of the dependency
     * @param <TProperties> the type of the properties to pass to the factory
     *
     * @throws UnknownDependencyException if the dependency is not found, invalid, or the caller context
     * does not have permission to access the dependency
     */
    @NotNull <TService, TProperties> TService get(@NotNull Class<TService> type, @Nullable TProperties properties);

    /**
     * Retrieve an instance from the container for the specified class type. Based on the service descriptor,
     * a dependency instance may be retrieved from the container cache, or a new instance is created.
     *
     * @param type the class type of the dependency
     * @param context the caller class that the container is being called from
     * @param properties the properties to create the instance with
     * @return the instance of the desired dependency type
     *
     * @param <TService> the type of the dependency
     * @param <TProperties> the type of the properties to pass to the factory
     *
     * @throws UnknownDependencyException if the dependency is not found, invalid, or the caller context
     * does not have permission to access the dependency
     */
    @NotNull <TService, TProperties> TService get(
        @NotNull Class<TService> type, @NotNull Class<?> context, @Nullable TProperties properties
    );

    /**
     * Register a dependency instance in the container cache for the specified class type.
     *
     * @param type the class type of the dependency
     * @param implementationType the type of the implementation
     *
     * @return the registered instance of the dependency
     *
     * @param <TService> the type of the dependency
     * @param <TImplementation> the type of the implementation
     */
    @NotNull <TService, TImplementation extends TService> TService implement(
        @NotNull Class<TService> type, @NotNull Class<TImplementation> implementationType
    );

    /**
     * Retrieve a global value from the container for the specified token.
     *
     * @param token the unique identifier of the value
     * @return the value of the desired token
     * @param <T> the type of the value
     *
     * @throws UnknownDependencyException if the value is not found, invalid, or the caller context
     */
    @NotNull <T> T get(@NotNull String token);

    /**
     * Manually update the value of the specified dependency type in the container cache.
     *
     * @param type the class type of the dependency
     * @param dependency the new instance of the dependency
     * @param <T> the type of the dependency
     */
    <T> void set(@NotNull Class<T> type, @NotNull T dependency);

    /**
     * Manually update the value of the specified dependency type in the container cache.
     *
     * @param type the class type of the dependency
     * @param dependency the new instance of the dependency
     * @param context the class that the dependency was instantiated for
     * @param <T> the type of the dependency
     */
    <T> void set(@NotNull Class<T> type, @NotNull T dependency, Class<?> context);

    /**
     * Update the value of the specified token in the container cache.
     *
     * @param token the unique identifier of the value
     * @param value the new value of the token
     * @param <T> the type of the value
     */
    <T> void set(@NotNull String token, @NotNull T value);

    /**
     * Check if the container has a dependency instance for the specified class type.
     *
     * @param type the class type of the dependency
     * @return true if the container has the dependency, otherwise false
     * @param <T> the type of the dependency
     */
    <T> boolean has(@NotNull Class<T> type);

    /**
     * Check if the container has a global value for the specified token.
     *
     * @param token the unique identifier of the value
     * @return true if the container has the value, otherwise false
     */
    boolean has(@NotNull String token);

    /**
     * Remove the dependency instance from the container cache for the specified class type.
     *
     * @param type the class type of the dependency
     * @param <T> the type of the dependency
     */
    <T> void unset(@NotNull Class<T> type);

    /**
     * Remove the dependency instance from the container cache for the specified class type.
     *
     * @param type the class type of the dependency
     * @param callback the callback that will be called with the dependency instance
     * @param <T> the type of the dependency
     */
    <T> void unset(@NotNull Class<T> type, @NotNull Consumer<T> callback);

    /**
     * Remove the global value from the container cache for the specified token.
     *
     * @param token the unique identifier of the value
     */
    void unset(@NotNull String token);

    /**
     * Reset the container instance to its initial state.
     * <p>
     * Invalidate the cache for all the registered dependencies and values.
     */
    void reset();
}
