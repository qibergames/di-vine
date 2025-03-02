package com.qibergames.divine;

import com.qibergames.divine.descriptor.generic.Service;
import com.qibergames.divine.descriptor.generic.ServiceLike;
import com.qibergames.divine.exception.InvalidServiceException;
import com.qibergames.divine.exception.ServiceInitializationException;
import com.qibergames.divine.provider.AnnotationProvider;
import com.qibergames.divine.provider.Ref;
import com.qibergames.divine.tree.cache.ContainerHook;
import com.qibergames.divine.exception.UnknownDependencyException;
import com.qibergames.divine.tree.ContainerProvider;
import com.qibergames.divine.tree.ContainerRegistry;
import com.qibergames.divine.impl.ClassLoaderContainerProvider;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Represents a container registry that instantiates and caches services globally and for specific scopes.
 * <p>
 * This class functions as a proxy to the {@link ContainerRegistry} class. For each call, it tries to resolve the
 * context that the container is being called from and then delegates the call to the appropriate container instance.
 * <p>
 * The contexts are resolved from the caller classes. By default, contexts are separated by class loaders. If you want
 * to have a custom implementation of the grouping mechanism, you can implement the {@link ContainerProvider} interface.
 */
@UtilityClass
public class Container {
    /**
     * The container provider that is used to create and manage container instances for specific contexts.
     */
    @Setter
    private @NotNull ContainerProvider provider = new ClassLoaderContainerProvider();

    /**
     * Retrieve a container registry by its specific name. The parent container is the root container of the hierarchy.
     * The retrieved container will be a child of the global container.
     * <p>
     * If the parent registry already contains a registry with the specified name, it will be returned.
     * Otherwise, a new registry will be created and registered in the parent registry.
     *
     * @param name the name of the registry
     * @return the container registry with the specified name
     */
    public @NotNull ContainerRegistry ofGlobal(@NotNull String name) {
        return ofGlobal().of(name);
    }

    /**
     * Retrieve a container registry by its specific name. The parent container is resolved from the current context.
     * The retrieved container will be a child of the container that is associated with the current context.
     * <p>
     * If the parent registry already contains a registry with the specified name, it will be returned.
     * Otherwise, a new registry will be created and registered in the parent registry.
     *
     * @param name the name of the registry
     * @return the container registry with the specified name
     */
    public @NotNull ContainerRegistry ofContext(@NotNull String name) {
        CallContext context = getContextContainer();
        return context.getContainer().of(name);
    }

    /**
     * Retrieve the root container registry of the container hierarchy.
     *
     * @return the global container registry
     */
    public @NotNull ContainerRegistry ofGlobal() {
        return provider.globalContainer();
    }

    /*
     * Retrieve the container registry of the current context.
     *
     * @return the container registry of the current context
     */
    public @NotNull ContainerRegistry ofContext() {
        return getContextContainer().getContainer();
    }

    /**
     * Register a container instance in the container hierarchy for the specified context.
     *
     * @param context the context that the container will be registered for
     * @param container the container instance to register
     */
    public void register(@NotNull Object context, @NotNull ContainerRegistry container) {
        provider.registerContainer(context, container);
    }

    /**
     * Register a hook that will be called when a dependency instance is being created.
     * The hook will be called with the dependency instance as an argument and should return the modified instance.
     *
     * @param id the unique identifier of the hook
     * @param hook the function that will be called when a dependency instance is being created
     */
    public void addHook(@NotNull String id, @NotNull ContainerHook hook) {
        CallContext context = getContextContainer();
        context.getContainer().addHook(id, hook);
    }

    /**
     * Remove a hook from the container instance.
     *
     * @param id the unique identifier of the hook
     */
    public void removeHook(@NotNull String id) {
        CallContext context = getContextContainer();
        context.getContainer().removeHook(id);
    }

    /**
     * Register a new custom annotation for the container instance with the specified implementation provider.
     *
     * @param annotation the custom annotation that will be registered
     * @param provider the implementation provider that will be called when the annotation is present
     *
     * @throws InvalidServiceException if the annotation does not have a RUNTIME retention
     */
    public void addProvider(
        @NotNull Class<? extends Annotation> annotation, @NotNull AnnotationProvider<?, ?> provider
    ) {
        CallContext context = getContextContainer();
        context.getContainer().addProvider(annotation, provider);
    }

    /**
     * Remove a custom annotation from the container instance.
     *
     * @param annotation the custom annotation that will be removed
     */
    public void removeProvider(@NotNull Class<? extends Annotation> annotation) {
        CallContext context = getContextContainer();
        context.getContainer().removeProvider(annotation);
    }

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
    public void insert(@NotNull @ServiceLike Class<?> @NotNull ... services) {
        CallContext context = getContextContainer();
        context.getContainer().insert(services);
    }

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
    public <TServices> @NotNull List<@NotNull TServices> getMany(@NotNull String id) {
        CallContext context = getContextContainer();
        return context.getContainer().getMany(id, context.getCaller());
    }

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
    public @NotNull <T> T get(@NotNull Class<T> type) {
        CallContext context = getContextContainer();
        return context.getContainer().get(type, context.getCaller());
    }

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
    public @NotNull <TService, TProperties> TService get(
        @NotNull Class<TService> type, @Nullable TProperties properties
    ) {
        CallContext context = getContextContainer();
        return context.getContainer().get(type, context.getCaller(), properties);
    }

    /**
     * Resolve the specified dependency from the container and pass it to the mapper function.
     *
     * @param type the class type of the dependency
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
    public <TResult, TService> TResult resolve(
        @NotNull Class<TService> type, @NotNull Function<@NotNull TService, TResult> mapper
    ) {
        CallContext context = getContextContainer();
        return context.getContainer().resolve(type, context.getCaller(), mapper);
    }

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
    public <TDependency, TResult> TResult resolve(
        @NotNull String token, @NotNull Function<TDependency, TResult> mapper
    ) {
        CallContext context = getContextContainer();
        return context.getContainer().resolve(token, mapper);
    }

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
    public <TService> @NotNull TService inspect(
        @NotNull Class<TService> type, @NotNull Consumer<@NotNull TService> callback
    ) {
        CallContext context = getContextContainer();
        return context.getContainer().inspect(type, callback);
    }

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
    public <TDependency> @NotNull TDependency inspect(@NotNull String token, @NotNull Consumer<TDependency> callback) {
        CallContext context = getContextContainer();
        return context.getContainer().inspect(token, callback);
    }

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
    public <TService> @NotNull Ref<TService> getRef(@NotNull Class<TService> type) {
        CallContext context = getContextContainer();
        return context.getContainer().getRef(type, context.getCaller());
    }

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
    public <TService, TImplementation extends TService> @NotNull TService implement(
        @NotNull Class<TService> type, @NotNull Class<TImplementation> implementationType
    ) {
        CallContext context = getContextContainer();
        return context.getContainer().implement(type, implementationType);
    }

    /**
     * Retrieve a global value from the container for the specified token.
     *
     * @param token the unique identifier of the value
     * @return the value of the desired token
     * @param <T> the type of the value
     *
     * @throws UnknownDependencyException if the value is not found, invalid, or the caller context
     */
    public @NotNull <T> T get(@NotNull String token) {
        CallContext context = getContextContainer();
        return context.getContainer().get(token);
    }

    /**
     * Manually update the value of the specified dependency type in the container cache.
     *
     * @param type the class type of the dependency
     * @param dependency the new instance of the dependency
     * @param <T> the type of the dependency
     */
    public <T> void set(@NotNull Class<T> type, @NotNull T dependency) {
        CallContext context = getContextContainer();
        context.getContainer().set(type, dependency, context.getCaller());
    }

    /**
     * Update the value of the specified token in the container cache.
     *
     * @param token the unique identifier of the value
     * @param value the new value of the token
     * @param <T> the type of the value
     */
    public <T> void set(@NotNull String token, @NotNull T value) {
        CallContext context = getContextContainer();
        context.getContainer().set(token, value);
    }

    /**
     * Check if the container has a dependency instance for the specified class type.
     *
     * @param type the class type of the dependency
     * @return true if the container has the dependency, otherwise false
     * @param <T> the type of the dependency
     */
    public <T> boolean has(@NotNull Class<T> type) {
        CallContext context = getContextContainer();
        return context.getContainer().has(type);
    }

    /**
     * Check if the container has a global value for the specified token.
     *
     * @param token the unique identifier of the value
     * @return true if the container has the value, otherwise false
     */
    public boolean has(@NotNull String token) {
        CallContext context = getContextContainer();
        return context.getContainer().has(token);
    }

    /**
     * Remove the dependency instance from the container cache for the specified class type.
     *
     * @param type the class type of the dependency
     * @param <T> the type of the dependency
     */
    public <T> void unset(@NotNull Class<T> type) {
        CallContext context = getContextContainer();
        context.getContainer().unset(type);
    }

    /**
     * Remove the dependency instance from the container cache for the specified class type.
     *
     * @param type the class type of the dependency
     * @param callback the callback that will be called with the dependency instance
     * @param <T> the type of the dependency
     */
    public <T> void unset(@NotNull Class<T> type, @NotNull Consumer<T> callback) {
        CallContext context = getContextContainer();
        context.getContainer().unset(type, callback);
    }

    /**
     * Remove the global value from the container cache for the specified token.
     *
     * @param token the unique identifier of the value
     */
    public void unset(@NotNull String token) {
        CallContext context = getContextContainer();
        context.getContainer().unset(token);
    }

    /**
     * Reset the container instance to its initial state.
     * <p>
     * Invalidate the cache for all the registered dependencies and values.
     */
    public void reset() {
        CallContext context = getContextContainer();
        context.getContainer().reset();
    }

    /**
     * Retrieve the json representation of the container hierarchy.
     *
     * @return the container tree exported as json
     */
    public @NotNull JsonObject export() {
        JsonObject json = new JsonObject();

        ContainerRegistry global = ofGlobal();
        json.addProperty("totalDependencies", global.getDependencyTree().size());
        json.addProperty("totalContainers", global.getContainerTree().size());
        json.add("global", global.export());

        return json;
    }

    /**
     * Ensure that the container class has been implemented by the framework core.
     *
     * @return the container registry implementation
     * @throws IllegalStateException if the container class is not implemented
     */
    private @NotNull CallContext getContextContainer() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        int skip = 0; // for now
        for (int i = skip; i < stackTrace.length; i++) {
            String className = stackTrace[i].getClassName();
            if (className.startsWith("com.atlas"))
                continue;
            try {
                Class<?> callerClass = Class.forName(className);
                return new CallContext(callerClass, provider.resolveContainer(callerClass));
            } catch (ClassNotFoundException ignored) {
            }
        }
        // fallback to this class, if no caller class is found
        return new CallContext(Container.class, provider.globalContainer());
    }

    /**
     * Represents a context that is used to resolve a container instance for a specific caller class.
     */
    @RequiredArgsConstructor
    @Getter
    private static class CallContext {
        /**
         * The class that called the container method.
         */
        private final @NotNull Class<?> caller;

        /**
         * The container instance that is used to resolve dependencies for the caller class.
         */
        private final @NotNull ContainerRegistry container;
    }
}
