package com.atlas.divine.tree;

import com.atlas.divine.Container;
import com.atlas.divine.descriptor.generic.Service;
import com.atlas.divine.descriptor.generic.ServiceLike;
import com.atlas.divine.exception.InvalidServiceException;
import com.atlas.divine.exception.ServiceInitializationException;
import com.atlas.divine.provider.AnnotationProvider;
import com.atlas.divine.tree.cache.ContainerHook;
import com.atlas.divine.exception.UnknownDependencyException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.util.List;
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
     * Register a new custom annotation for the container instance with the specified implementation provider.
     *
     * @param annotation the custom annotation that will be registered
     * @param provider the implementation provider that will be called when the annotation is present
     *
     * @throws InvalidServiceException if the annotation does not have a RUNTIME retention
     */
    void addProvider(@NotNull Class<? extends Annotation> annotation, @NotNull AnnotationProvider<?> provider);

    /**
     * Remove a custom annotation from the container instance.
     *
     * @param annotation the custom annotation that will be removed
     */
    void removeProvider(@NotNull Class<? extends Annotation> annotation);

    /**
     * Register services in the container, that specify {@link Service#multiple()} = {@code true} in their descriptor.
     * <p>
     * The services will be registered with their unique identifier, as specified in {@link Service#id()}.
     * <p>
     * You can later look up these services using the {@link Container#getMany(String)} method.
     *
     * @param services the classes of the services to register
     *
     * @throws InvalidServiceException if the service does not specify multiple correctly, or the service is invalid
     */
    void insert(@NotNull @ServiceLike Class<?> @NotNull ... services);

    /**
     * Retrieve multiple instances from the container for the specified unique identifier.
     *
     * @param id the unique identifier, that the services are grouped by
     * @param context the caller class that the container is being called from
     * @return the list of instances of the desired dependency identifier
     *
     * @param <TServices> the base type of the services
     *
     * @throws InvalidServiceException if the service descriptor is invalid or the service type cannot be a service
     * @throws ServiceInitializationException if an error occurs while initializing the service
     */
    <TServices> @NotNull List<@NotNull TServices> getMany(@NotNull String id, @NotNull Class<?> context);

    /**
     * Retrieve multiple instances from the container for the specified unique identifier.
     *
     * @param id the unique identifier, that the services are grouped by
     * @return the list of instances of the desired dependency identifier
     *
     * @param <TServices> the base type of the services
     *
     * @throws InvalidServiceException if the service descriptor is invalid or the service type cannot be a service
     * @throws ServiceInitializationException if an error occurs while initializing the service
     */
    <TServices> @NotNull List<@NotNull TServices> getMany(@NotNull String id);

    /**
     * Retrieve an instance from the container for the specified class type. Based on the service descriptor,
     * a dependency instance may be retrieved from the container cache, or a new instance is created.
     *
     * @param type the class type of the dependency
     * @return the instance of the desired dependency type
     * @param <T> the type of the dependency
     *
     * @throws InvalidServiceException if the service descriptor is invalid or the service type cannot be a service
     * @throws ServiceInitializationException if an error occurs while initializing the service
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
     * @throws InvalidServiceException if the service descriptor is invalid or the service type cannot be a service
     * @throws ServiceInitializationException if an error occurs while initializing the service
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
     * @throws InvalidServiceException if the service descriptor is invalid or the service type cannot be a service
     * @throws ServiceInitializationException if an error occurs while initializing the service
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
     * @throws InvalidServiceException if the service descriptor is invalid or the service type cannot be a service
     * @throws ServiceInitializationException if an error occurs while initializing the service
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
     *
     * @throws InvalidServiceException if the service descriptor is invalid or the service type cannot be a service
     * @throws ServiceInitializationException if an error occurs while initializing the service
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
