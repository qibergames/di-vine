package com.qibergames.divine.tree;

import com.qibergames.divine.Container;
import com.qibergames.divine.descriptor.generic.Service;
import com.qibergames.divine.descriptor.generic.ServiceLike;
import com.qibergames.divine.exception.InvalidServiceException;
import com.qibergames.divine.exception.ServiceInitializationException;
import com.qibergames.divine.method.MethodInspector;
import com.qibergames.divine.provider.AnnotationProvider;
import com.qibergames.divine.provider.Ref;
import com.qibergames.divine.tree.cache.ContainerHook;
import com.qibergames.divine.exception.UnknownDependencyException;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.CheckReturnValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

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
    <TAnnotation extends Annotation> void addProvider(
        @NotNull Class<TAnnotation> annotation, @NotNull AnnotationProvider<?, TAnnotation> provider
    );

    /**
     * Remove a custom annotation from the container instance.
     *
     * @param annotation the custom annotation that will be removed
     */
    void removeProvider(@NotNull Class<? extends Annotation> annotation);

    /**
     * Register a new method inspector for the specified annotation.
     * <p>
     * The inspector will be notified of each method upon service instantiation, that specify this annotation.
     *
     * @param annotation the annotation that marks methods to be inspected
     * @param inspector the method inspection callback
     */
    <TAnnotation extends Annotation> void addInspector(
        @NotNull Class<TAnnotation> annotation, @NotNull MethodInspector<TAnnotation> inspector
    );

    /**
     * Remove a method inspector for the specified annotation.
     *
     * @param annotation the annotation that marks methods to be inspected
     */
    void removeInspector(@NotNull Class<? extends Annotation> annotation);

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
    <T> @NotNull T get(@NotNull Class<T> type);

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
    <T> @NotNull T get(@NotNull Class<T> type, @NotNull Class<?> context);

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
    <TService, TProperties> @NotNull TService get(@NotNull Class<TService> type, @Nullable TProperties properties);

    /**
     * Resolve the specified dependency from the container and pass it to the mapper function.
     *
     * @param type the class type of the dependency
     * @param context the caller class that the container is being called from
     * @param mapper the function to transform the dependency value with
     *
     * @return the transformed dependency value
     *
     * @param <TResult> the type of the transformed value
     * @param <TService> the type of the dependency
     *
     * @throws InvalidServiceException if the service descriptor is invalid or the service type cannot be a service
     * @throws ServiceInitializationException if an error occurs while initializing the service
     */
    @CheckReturnValue
    <TResult, TService> @NotNull TResult resolve(
        @NotNull Class<TService> type, @NotNull Class<?> context, @NotNull Function<@NotNull TService, TResult> mapper
    );

    /**
     * Resolve the specified dependency from the container and pass it to the mapper function.
     *
     * @param token the token of the dependency
     * @param mapper the function to transform the dependency value with
     *
     * @return the transformed dependency value
     *
     * @param <TResult> the type of the transformed value
     *
     * @throws InvalidServiceException if the service descriptor is invalid or the service type cannot be a service
     * @throws ServiceInitializationException if an error occurs while initializing the service
     */
    <TDependency, TResult> @NotNull TResult resolve(
        @NotNull String token, @NotNull Function<TDependency, TResult> mapper
    );

    /**
     * Get the specified dependency from the container and pass it to the callback consumer.
     *
     * @param type the class type of the dependency
     * @param callback the consumer to invoke with the dependency instance
     *
     * @return the dependency value
     *
     * @param <TService> the type of the dependency
     *
     * @throws InvalidServiceException if the service descriptor is invalid or the service type cannot be a service
     * @throws ServiceInitializationException if an error occurs while initializing the service
     */
    @CanIgnoreReturnValue
    <TService> @NotNull TService inspect(@NotNull Class<TService> type, @NotNull Consumer<@NotNull TService> callback);

    /**
     * Get the specified dependency from the container and pass it to the callback consumer.
     *
     * @param token the token of the dependency
     * @param callback the consumer to invoke with the dependency instance
     *
     * @return the dependency value
     *
     * @param <TDependency> the type of the dependency value
     *
     * @throws InvalidServiceException if the service descriptor is invalid or the service type cannot be a service
     * @throws ServiceInitializationException if an error occurs while initializing the service
     * @throws UnknownDependencyException if the requested dependency is not present in the container
     */
    @CanIgnoreReturnValue
    <TDependency> @NotNull TDependency inspect(@NotNull String token, @NotNull Consumer<TDependency> callback);

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
    <TService, TProperties> @NotNull TService get(
        @NotNull Class<TService> type, @NotNull Class<?> context, @Nullable TProperties properties
    );

    /**
     * Create a reference to a dependency that will be lazily initialized when the reference is accessed.
     * <p>
     * This interface is used to inject dependencies into a class, without actually retrieving the dependency.
     * The dependency will be retrieved when the {@link Ref#get()} method is called.
     * <p>
     * This feature is useful, when you only want to instantiate a dependency during runtime, after service initialization.
     * <p>
     * Using {@link Ref} can fix circular dependencies, by allowing you to lazily inject a dependency.
     *
     * @param type the class type of the dependency
     * @return the reference to the desired dependency type
     *
     * @param <TService> the type of the dependency
     */
    <TService> @NotNull Ref<TService> getRef(@NotNull Class<TService> type);

    /**
     * Create a reference to a dependency that will be lazily initialized when the reference is accessed.
     * <p>
     * This interface is used to inject dependencies into a class, without actually retrieving the dependency.
     * The dependency will be retrieved when the {@link Ref#get()} method is called.
     * <p>
     * This feature is useful, when you only want to instantiate a dependency during runtime, after service initialization.
     * <p>
     * Using {@link Ref} can fix circular dependencies, by allowing you to lazily inject a dependency.
     *
     * @param type the class type of the dependency
     * @param context the caller class that the container is being called from
     * @return the reference to the desired dependency type
     *
     * @param <TService> the type of the dependency
     */
    <TService> @NotNull Ref<TService> getRef(@NotNull Class<TService> type, @NotNull Class<?> context);

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
    <T> @NotNull T get(@NotNull String token);

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
