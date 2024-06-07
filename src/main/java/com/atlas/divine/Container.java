package com.atlas.divine;

import com.atlas.divine.descriptor.generic.Service;
import com.atlas.divine.descriptor.generic.ServiceLike;
import com.atlas.divine.provider.AnnotationProvider;
import com.atlas.divine.tree.cache.ContainerHook;
import com.atlas.divine.exception.UnknownDependencyException;
import com.atlas.divine.tree.ContainerProvider;
import com.atlas.divine.tree.ContainerRegistry;
import com.atlas.divine.impl.ClassLoaderContainerProvider;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.function.Consumer;

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
     * Retrieve a container registry by its specific name.
     * If the parent registry already contains a registry with the specified name, it will be returned.
     * Otherwise, a new registry will be created and registered in the parent registry.
     *
     * @param name the name of the registry
     * @return the container registry with the specified name
     */
    public @NotNull ContainerRegistry of(@NotNull String name) {
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
     */
    public void addProvider(@NotNull Class<? extends Annotation> annotation, @NotNull AnnotationProvider<?> provider) {
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
     * @throws UnknownDependencyException if the dependency is not found, invalid, or the caller context
     * does not have permission to access the dependency
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
     * @throws UnknownDependencyException if the dependency is not found, invalid, or the caller context
     * does not have permission to access the dependency
     */
    public @NotNull <TService, TProperties> TService get(
        @NotNull Class<TService> type, @Nullable TProperties properties
    ) {
        CallContext context = getContextContainer();
        return context.getContainer().get(type, context.getCaller(), properties);
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
