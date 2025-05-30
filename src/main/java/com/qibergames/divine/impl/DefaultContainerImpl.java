package com.qibergames.divine.impl;

import com.qibergames.divine.Container;
import com.qibergames.divine.exception.*;
import com.qibergames.divine.inspector.MethodInspector;
import com.qibergames.divine.inspector.ServiceMethod;
import com.qibergames.divine.provider.*;
import com.qibergames.divine.runtime.lazy.LazyFieldAccess;
import com.qibergames.divine.tree.cache.ContainerHook;
import com.qibergames.divine.tree.cache.Dependency;
import com.qibergames.divine.descriptor.generic.*;
import com.qibergames.divine.runtime.lifecycle.AfterInitialized;
import com.qibergames.divine.runtime.lifecycle.BeforeTerminate;
import com.qibergames.divine.descriptor.property.PropertyProvider;
import com.qibergames.divine.runtime.context.Contexts;
import com.qibergames.divine.descriptor.factory.Factory;
import com.qibergames.divine.descriptor.factory.NoFactory;
import com.qibergames.divine.descriptor.implementation.NoImplementation;
import com.qibergames.divine.descriptor.property.NoProperties;
import com.qibergames.divine.descriptor.property.NoPropertiesProvider;
import com.qibergames.divine.tree.ContainerRegistry;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Represents a scope specific container instance that manages dependencies and token values.
 */
@RequiredArgsConstructor
public class DefaultContainerImpl implements ContainerRegistry {
    /**
     * The static counter that increments the container identifier for each new container instance.
     */
    private static final @NotNull AtomicInteger CONTAINER_ID = new AtomicInteger(0);

    /**
     * The map of the registered dependency implementations in the container.
     */
    private final @NotNull Map<@NotNull Class<?>, @NotNull CachedDependency<?>> dependencies =
        new ConcurrentHashMap<>();

    /**
     * The map of the globally registered values in the container.
     */
    private final @NotNull Map<@NotNull String, @NotNull Object> values = new ConcurrentHashMap<>();

    /**
     * The map of the sub-containers registered to this container.
     */
    private final @NotNull Map<@NotNull String, @NotNull DefaultContainerImpl> containers =
        new ConcurrentHashMap<>();

    /**
     * The map of the hooks registered to this container that can modify dependencies on creation.
     */
    private final @NotNull Map<@NotNull String, @NotNull ContainerHook> hooks = new ConcurrentHashMap<>();

    /**
     * The map of the registered implementation providers for custom annotations.
     */
    private final @NotNull Map<@NotNull Class<? extends Annotation>, @NotNull AnnotationProvider<?, ?>> providers =
        new ConcurrentHashMap<>();

    /**
     * The map of the registered service method inspector callbacks for the specified annotations.
     */
    private final @NotNull Map<@NotNull Class<? extends Annotation>, @NotNull MethodInspector<?>> inspectors =
        new ConcurrentHashMap<>();

    /**
     * The map of registered services that are grouped by their unique identifier.
     */
    private final @NotNull Map<@NotNull String, @NotNull List<@NotNull Class<?>>> multiServices =
        new ConcurrentHashMap<>();

    /**
     * The stack of classes that are currently being resolved in the container.
     */
    private final @NotNull ThreadLocal<@NotNull Stack<@NotNull Class<?>>> resolvingStack =
        ThreadLocal.withInitial(Stack::new);

    /**
     * The map of fields to be lazily injected by the container.
     * <p>
     * All fields annotated with {@link Inject} that specify {@code lazy = true} will be injected by the container,
     * after the whole dependency tree for a service is resolved.
     */
    private final @NotNull ThreadLocal<@NotNull Map<@NotNull Field, @NotNull LazyFieldAccess>> lazyFields =
        ThreadLocal.withInitial(LinkedHashMap::new);

    /**
     * The indication, whether the container is currently injecting lazy fields.
     */
    private final @NotNull ThreadLocal<@NotNull Boolean> injectingLazyFields = ThreadLocal.withInitial(() -> false);

    /**
     * The map of initialization methods to be lazily invoked by the container.
     * <p>
     * All methods annotated with {@link AfterInitialized} that specify {@code lazy = true} will be invoked by the
     * container, after the whole dependency tree for a service is resolved.
     */
    private final @NotNull ThreadLocal<@NotNull Map<@NotNull Method, @NotNull Object>> lazyMethods =
        ThreadLocal.withInitial(LinkedHashMap::new);

    /**
     * The indication, whether the container is currently invoking lazy methods.
     */
    private final @NotNull ThreadLocal<@NotNull Boolean> invokingLazyMethods = ThreadLocal.withInitial(() -> false);

    /**
     * The root container of the container hierarchy. It is {@code null} if {@code this} container is the root.
     */
    private final @Nullable ContainerRegistry rootContainer;

    /**
     * The unique identifier of the container instance.
     */
    @Getter
    private final @NotNull String name;

    /**
     * Initialize the container instance with the specified root container.
     *
     * @param rootContainer the root container of the container hierarchy
     */
    public DefaultContainerImpl(@Nullable ContainerRegistry rootContainer) {
        this.rootContainer = rootContainer;
        name = "container-" + CONTAINER_ID.incrementAndGet();
    }

    /**
     * Retrieve a container registry by its specific name.
     * If the parent registry already contains a registry with the specified name, it will be returned.
     * Otherwise, a new registry will be created and registered in the parent registry.
     *
     * @param name the name of the registry
     * @return the container registry with the specified name
     */
    @Override
    public @NotNull ContainerRegistry of(@NotNull String name) {
        // resolve the container by name from the cache
        DefaultContainerImpl container = containers.get(name);
        // create the container if it does not exist
        if (container == null) {
            container = new DefaultContainerImpl(rootContainer != null ? rootContainer : this, name);
            containers.put(name, container);
        }
        return container;
    }

    /**
     * Retrieve the root container registry of the container hierarchy.
     *
     * @return the global container registry
     */
    @Override
    public @NotNull ContainerRegistry ofGlobal() {
        return rootContainer != null ? rootContainer : this;
    }

    /**
     * Retrieve the list of dependencies that are registered in the container hierarchy.
     *
     * @return the list of all dependencies in the container hierarchy
     */
    @Override
    public @NotNull List<Dependency> getDependencyTree() {
        return getTotalDependencies()
            .stream()
            .map(value -> new Dependency(value.value(), value.descriptor(), value.context()))
            .collect(Collectors.toList());
    }

    /**
     * Retrieve the list of container instances that are registered in the container hierarchy.
     *
     * @return the set of all container instances in the container hierarchy
     */
    @Override
    public @NotNull Set<@NotNull ContainerRegistry> getContainerTree() {
        // make sure to start resolving the tree from the root container
        if (rootContainer instanceof DefaultContainerImpl)
            return rootContainer.getContainerTree();

        // recursively resolve the containers from the child containers
        return getLocalContainers();
    }

    /**
     * Register a hook that will be called when a dependency instance is being created.
     * The hook will be called with the dependency instance as an argument and should return the modified instance.
     *
     * @param id the unique identifier of the hook
     * @param hook the function that will be called when a dependency instance is being created
     */
    @Override
    public void addHook(@NotNull String id, @NotNull ContainerHook hook) {
        hooks.put(id, hook);
    }

    /**
     * Remove a hook from the container instance.
     *
     * @param id the unique identifier of the hook
     */
    @Override
    public void removeHook(@NotNull String id) {
        hooks.remove(id);
    }

    /**
     * Register a new custom annotation for the container instance with the specified implementation provider.
     *
     * @param annotation the custom annotation that will be registered
     * @param provider the implementation provider that will be called when the annotation is present
     *
     * @throws InvalidServiceException if the annotation does not have a RUNTIME retention
     */
    @Override
    public <TAnnotation extends Annotation> void addProvider(
        @NotNull Class<TAnnotation> annotation, @NotNull AnnotationProvider<?, TAnnotation> provider
    ) {
        Retention retention = annotation.getAnnotation(Retention.class);
        if (retention == null || retention.value() != RetentionPolicy.RUNTIME)
            throw new InvalidServiceException(
                "Annotation " + annotation.getName() + " must have a RUNTIME retention"
            );
        providers.put(annotation, provider);
    }

    /**
     * Remove a custom annotation from the container instance.
     *
     * @param annotation the custom annotation that will be removed
     */
    @Override
    public void removeProvider(@NotNull Class<? extends Annotation> annotation) {
        providers.remove(annotation);
    }

    /**
     * Register a new method inspector for the specified annotation.
     * <p>
     * The inspector will be notified of each method upon service instantiation, that specify this annotation.
     *
     * @param annotation the annotation that marks methods to be inspected
     * @param inspector the method inspection callback
     */
    @Override
    public <TAnnotation extends Annotation> void addInspector(
        @NotNull Class<TAnnotation> annotation, @NotNull MethodInspector<TAnnotation> inspector
    ) {
        Retention retention = annotation.getAnnotation(Retention.class);
        if (retention == null || retention.value() != RetentionPolicy.RUNTIME)
            throw new InvalidServiceException(
                "Annotation " + annotation.getName() + " must have a RUNTIME retention"
            );
        inspectors.put(annotation, inspector);
    }

    /**
     * Remove a method inspector for the specified annotation.
     *
     * @param annotation the annotation that marks methods to be inspected
     */
    @Override
    public void removeInspector(@NotNull Class<? extends Annotation> annotation) {
        inspectors.remove(annotation);
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
    @Override
    public void insert(@NotNull @ServiceLike Class<?> @NotNull ... services) {
        // iterate over the services to insert
        for (Class<?> service : services) {
            // resolve the service descriptor of the service type
            Service descriptor = resolveDescriptor(service, true);
            // check if the service is allowed to be inserted
            if (!descriptor.multiple())
                throw new InvalidServiceException(
                    "Service " + service.getName() + " does not have multiple set to true"
                );

            // validate that the service has a unique identifier
            String id = descriptor.id();
            if (id.equals(Service.DEFAULT_ID))
                throw new InvalidServiceException(
                    "Service " + service.getName() + " does not have a unique identifier"
                );

            // register the service in the container
            multiServices.computeIfAbsent(id, key -> new ArrayList<>()).add(service);
        }
    }

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
    @Override
    public <TServices> @NotNull List<@NotNull TServices> getMany(@NotNull String id, @NotNull Class<?> context) {
        List<TServices> services = new ArrayList<>();
        // iterate over each service registered with the specified identifier
        for (Class<?> service : multiServices.getOrDefault(id, new ArrayList<>())) {
            // retrieve the instance of the service type
            @SuppressWarnings("unchecked")
            TServices instance = (TServices) get(service, context, null, true);
            services.add(instance);
        }
        return services;
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
    @Override
    public <TServices> @NotNull List<@NotNull TServices> getMany(@NotNull String id) {
        try {
            return getMany(id, Contexts.getCallerClass(Thread.currentThread().getStackTrace()));
        } catch (ClassNotFoundException e) {
            return getMany(id, Container.class);
        }
    }

    /**
     * Retrieve an instance from the container for the specified class type. Based on the service descriptor,
     * a dependency instance may be retrieved from the container cache, or a new instance is created.
     *
     * @param type the class type of the dependency
     * @return the instance of the desired dependency type
     *
     * @throws InvalidServiceException if the service descriptor is invalid or the service type cannot be a service
     * @throws ServiceInitializationException if an error occurs while initializing the service
     */
    @Override
    public <T> @NotNull T get(@NotNull Class<T> type) {
        try {
            return get(type, Contexts.getCallerClass(Thread.currentThread().getStackTrace()));
        } catch (ClassNotFoundException e) {
            return get(type, Container.class);
        }
    }

    /**
     * Retrieve an instance from the container for the specified class type. Based on the service descriptor,
     * a dependency instance may be retrieved from the container cache, or a new instance is created.
     *
     * @param type the class type of the dependency
     * @param context the caller class that the container is being called from
     * @return the instance of the desired dependency type
     *
     * @throws InvalidServiceException if the service descriptor is invalid or the service type cannot be a service
     * @throws ServiceInitializationException if an error occurs while initializing the service
     */
    @Override
    public <T> @NotNull T get(@NotNull Class<T> type, @NotNull Class<?> context) {
        return get(type, context, null);
    }

    /**
     * Retrieve an instance from the container for the specified class type. Based on the service descriptor,
     * a dependency instance may be retrieved from the container cache, or a new instance is created.
     *
     * @param type the class type of the dependency
     * @param properties the properties to create the instance with
     * @return the instance of the desired dependency type
     *
     * @throws InvalidServiceException if the service descriptor is invalid or the service type cannot be a service
     * @throws ServiceInitializationException if an error occurs while initializing the service
     */
    @Override
    public <TService, TProperties> @NotNull TService get(
        @NotNull Class<TService> type, @Nullable TProperties properties
    ) {
        try {
            return get(type, Contexts.getCallerClass(Thread.currentThread().getStackTrace()), properties);
        } catch (ClassNotFoundException e) {
            return get(type, Container.class, properties);
        }
    }

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
    @Override
    public <TResult, TService> @NotNull TResult resolve(
        @NotNull Class<TService> type, @NotNull Class<?> context, @NotNull Function<@NotNull TService, TResult> mapper
    ) {
        return mapper.apply(get(type, context));
    }

    /**
     * Resolve the specified dependency from the container and pass it to the mapper function.
     *
     * @param token the token of the dependency
     * @param mapper the function to transform the dependency value with
     *
     * @return the transformed dependency value
     *
     * @throws InvalidServiceException if the service descriptor is invalid or the service type cannot be a service
     * @throws ServiceInitializationException if an error occurs while initializing the service
     */
    @Override
    public <TDependency, TResult> @NotNull TResult resolve(
        @NotNull String token, @NotNull Function<TDependency, TResult> mapper
    ) {
        return mapper.apply(get(token));
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
        TService dependency = get(type);
        callback.accept(dependency);
        return dependency;
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
        TDependency dependency = get(token);
        callback.accept(dependency);
        return dependency;
    }


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
    @Override
    public <TService, TProperties> @NotNull TService get(
        @NotNull Class<TService> type, @NotNull Class<?> context, @Nullable TProperties properties
    ) {
        return get(type, context, properties, false);
    }

    /**
     * Retrieve an instance from the container for the specified class type. Based on the service descriptor,
     * a dependency instance may be retrieved from the container cache, or a new instance is created.
     *
     * @param type the class type of the dependency
     * @param context the caller class that the container is being called from
     * @param properties the properties to create the instance with
     * @param allowMultiple whether to allow services that specifies {@link Service#multiple()} = {@code true}
     * @return the instance of the desired dependency type
     *
     * @param <TService> the type of the dependency
     * @param <TProperties> the type of the properties to pass to the factory
     *
     * @throws InvalidServiceException if the service descriptor is invalid or the service type cannot be a service
     * @throws ServiceInitializationException if an error occurs while initializing the service
     */
    public <TService, TProperties> @NotNull TService get(
        @NotNull Class<TService> type, @NotNull Class<?> context, @Nullable TProperties properties,
        boolean allowMultiple
    ) {
        // get the stack of classes that are currently being resolved in the container
        Stack<Class<?>> stack = resolvingStack.get();

        // check if the requested dependency is already initialized in the dependency tree
        if (stack.contains(type))
            throw new CircularDependencyException(
                "Circular dependency detected for service " + type.getName() + " in context " + context.getName() +
                ". If you are certain, this is not a bug, consider using Ref<T>, or @Inject(lazy=true) to lazily " +
                "inject the dependency."
            );

        // push the current class type to the stack
        stack.push(type);

        // resolve the dependency tree from the container
        try {
            return resolveDependency(type, context, properties, allowMultiple);
        }
        // re-throw generic service exceptions
        catch (GenericServiceException e) {
            throw e;
        }
        // convert all non-dependency-injector exceptions to a service initialization exception,
        // as the user should only be aware of the known exceptions
        catch (Exception e) {
            throw new ServiceInitializationException(
                "Error whilst initializing service " + type.getName() + " in context " + context.getName(), e
            );
        }
        // finally clean up the stack and inject lazy field dependencies
        finally {
            stack.pop();
            injectLazyFields();
            invokeLazyMethods();
        }
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
    @Override
    public <TService> @NotNull Ref<TService> getRef(@NotNull Class<TService> type) {
        return () -> get(type);
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
     * @param context the caller class that the container is being called from
     * @return the reference to the desired dependency type
     *
     * @param <TService> the type of the dependency
     */
    @Override
    public <TService> @NotNull Ref<TService> getRef(@NotNull Class<TService> type, @NotNull Class<?> context) {
        return () -> get(type, context);
    }

    /**
     * Resolve the type of the dependency that is specified by the {@link Ref}'s generic type.
     *
     * @param genericType the generic type of the reference
     * @return the resolved class type of the dependency
     */
    private @NotNull Class<?> resolveReferenceType(@NotNull Type genericType) {
        // validate that the generic type is a parameterized type
        Type typeArgument = getTypeArgument(genericType);

        // convert the type argument to a class type
        if (typeArgument instanceof Class<?>)
            return (Class<?>) typeArgument;
        else if (typeArgument instanceof ParameterizedType)
            return (Class<?>) ((ParameterizedType) typeArgument).getRawType();
        else
            throw new InvalidServiceAccessException(
                "Generic type " + genericType + " must have a class type as its actual type argument"
            );
    }

    /**
     * Resolve the first type argument of the specified type.
     *
     * @param genericType the generic type to check
     * @return the first type argument of the type
     */
    private static Type getTypeArgument(@NotNull Type genericType) {
        if (!(genericType instanceof ParameterizedType)) throw new InvalidServiceAccessException(
            "Ref<T> generic type " + genericType + " must be a parameterized type"
        );

        // validate that the generic type has one actual type argument
        Type[] typeArguments = ((ParameterizedType) genericType).getActualTypeArguments();
        if (typeArguments.length != 1) throw new InvalidServiceAccessException(
            "Ref<T> generic type " + genericType + " must have one actual type argument"
        );

        // resolve the actual type argument of the generic type
        return typeArguments[0];
    }

    /**
     * Retrieve an instance from the container for the specified class type. Based on the service descriptor,
     * a dependency instance may be retrieved from the container cache, or a new instance is created.
     *
     * @param type the class type of the dependency
     * @param context the caller class that the container is being called from
     * @param properties the properties to create the instance with
     * @param allowMultiple whether to allow services that specifies {@link Service#multiple()} = {@code true}
     * @return the instance of the desired dependency type
     *
     * @param <TService> the type of the dependency
     * @param <TProperties> the type of the properties to pass to the factory
     *
     * @throws InvalidServiceException if the service descriptor is invalid or the service type cannot be a service
     * @throws ServiceInitializationException if an error occurs while initializing the service
     */
    public <TService, TProperties> @NotNull TService resolveDependency(
        @NotNull Class<TService> type, @NotNull Class<?> context, @Nullable TProperties properties,
        boolean allowMultiple
    ) {
        // resolve the service descriptor of the dependency type
        Service service = resolveDescriptor(type, true);

        // validate that the service is not registered as multiple, if the caller does not allow it
        if (service.multiple() && !allowMultiple)
            throw new InvalidServiceAccessException(
                "Service " + type.getName() + " is registered as multiple, use getMany instead"
            );

        // resolve the dependency from the root container if it has a singleton scope
        ServiceScope scope = service.scope();
        if (scope == ServiceScope.SINGLETON && rootContainer != null)
            return rootContainer.get(type, context);
        // if the root container is null, that means that the current container is the root, fall through the next case

        // create an instance each time the dependency is accessed
        else if (scope == ServiceScope.TRANSIENT)
            return createInstance(type, service, context, properties);

        // let `CONTAINER` scope fall through to the default case

        // validate that the context class has permission to access the service type
        checkAccess(type, service.visibility(), context);

        // return the cached instance of the service, or create a new one if it does not exist
        return getCachedOrCreate(type, service, context, properties);
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
    @Override
    public <TService, TImplementation extends TService> @NotNull TService implement(
        @NotNull Class<TService> type, @NotNull Class<TImplementation> implementationType
    ) {
        // check if the service does not allow the implementation type to be associated as
        checkPermit: {
            // resolve the descriptor of the target class. validation is disabled here, as we don't need to check
            // if the target type can be initialized, because the implementation is the initializer
            Service descriptor = resolveDescriptor(type, false);
            @NotNull @ServiceLike Class<?>[] permits = descriptor.permits();

            // stop checking, if the descriptor allows any types to implement the service
            if (permits.length == 0)
                break checkPermit;

            // stop checking, if the descriptor allows the implementation type to implement the service
            for (Class<?> permit : permits) {
                if (permit.equals(implementationType))
                    break checkPermit;
            }

            // descriptor disallows the implementation, throw an exception
            throw new InvalidServiceAccessException(
                "Service " + type.getName() + " does not permit implementation " + implementationType.getName()
            );
        }

        // instantiate the implementation type
        TImplementation implementation = Container.get(implementationType);
        // associate the target type with the implementation instance
        Container.set(type, implementation);
        return implementation;
    }

    /**
     * Resolve the service descriptor of the specified dependency type.
     *
     * @param type the class type of the dependency
     * @return the service descriptor of the dependency type
     * @param <T> the type of the dependency
     *
     * @throws InvalidServiceException if the dependency type is not a service or the type cannot be a service
     */
    private <T> @NotNull Service resolveDescriptor(
        @NotNull Class<T> type, boolean validate
    ) throws InvalidServiceException {
        // validate that the service type annotates the service descriptor annotation
        Service service = type.getAnnotation(Service.class);
        if (service == null)
            throw new InvalidServiceException("Class " + type.getName() + " is not a service");

        // return if the parent context does not require service descriptor validation
        if (!validate)
            return service;

        // make sure not to use annotation types for dependencies
        if (type.isAnnotation())
            throw new InvalidServiceException("Annotation type " + type.getName() + " cannot be a service");

        // validate that the service type has a factory specified if it is an enum
        if (type.isEnum() && service.factory() == NoFactory.class) {
            if (service.scope() == ServiceScope.TRANSIENT)
                throw new InvalidServiceException(
                    "Transient enum service type " + type.getName() + " must have a factory specified"
                );

            if (!has(type))
                throw new InvalidServiceException(
                    "Enum service type " + type.getName() + " must have a factory specified in the service " +
                    "descriptor, or it must be already initialized in the container"
                );
        }

        // validate that the service type has a factory or an implementation specified if it is an interface
        if (
            type.isInterface() && service.factory() == NoFactory.class &&
            service.implementation() == NoImplementation.class && !has(type)
        )
            throw new InvalidServiceException(
                "Interface service type " + type.getName() + " must have a factory or an implementation specified in " +
                "the service descriptor, or it must be already initialized in the container"
            );

        return service;
    }

    /**
     * Retrieve the cached instance of the specified service type, or create a new one if it does not exist.
     *
     * @param type the class type of the dependency
     * @param service the dependency service descriptor
     * @param context the class that requested the dependency
     * @param properties the properties to create the instance with
     * @return the instance of the desired dependency type
     *
     * @param <TService> the type of the dependency
     * @param <TProperties> the type of the properties to pass to the factory
     *
     * @throws InvalidServiceException if the service descriptor is invalid or the service type cannot be a service
     * @throws ServiceInitializationException if an error occurs while initializing the service
     */
    private <TService, TProperties> @NotNull TService getCachedOrCreate(
        @NotNull Class<TService> type, @NotNull Service service, @NotNull Class<?> context,
        @Nullable TProperties properties
    ) {
        // resolve the dependency from the container cache
        CachedDependency<?> cachedDependency = dependencies.get(type);
        if (cachedDependency != null)
            return type.cast(cachedDependency.value());

        // instantiate the service for the current context
        TService instance = createInstance(type, service, context, properties);
        // service has container scope, cache it in the container
        dependencies.put(type, new CachedDependency<>(instance, service, context, resolveTerminators(type)));

        return instance;
    }

    /**
     * Create an instance of the specified service type.
     *
     * @param type the type of the dependency to be instantiated
     * @param descriptor the dependency service descriptor
     * @param context the class that requested the dependency
     * @return a new instance of the service type
     *
     * @param <TService> the type of the dependency
     * @param <TProperties> the type of the properties to pass to the factory
     *
     * @throws ServiceInitializationException if an error occurs while initializing the service
     */
    @SuppressWarnings("unchecked")
    private <TService, TProperties> @NotNull TService createInstance(
        @NotNull Class<TService> type, @NotNull Service descriptor, @NotNull Class<?> context,
        @Nullable TProperties properties
    ) {
        TService value;
        Class<?> implementation = descriptor.implementation();

        // validate that the properties match the factory of the service
        validateProperties(descriptor, properties);

        // resolve the factory from the service descriptor
        // use the factory to instantiate the service, if it is specified
        Factory<TService, TProperties> factory = createFactory(descriptor.factory());
        if (factory != null)
            value = factory.create(descriptor, type, context, properties);

        // use the implementation of the service, if it is specified
        else if (implementation != NoImplementation.class) {
            if (!type.isAssignableFrom(implementation))
                throw new InvalidServiceException(
                    "Service " + descriptor + " implementation " + implementation.getName() +
                    " does not implement the service type " + type.getName()
                );
            type = (Class<TService>) implementation;
            value = createInstanceWithDependencies(type, context);
        }
        // let the dependency injector instantiate the service and resolve its constructor dependencies
        else
            value = createInstanceWithDependencies(type, context);

        injectService(descriptor, type, value, context);

        return value;
    }

    /**
     * Inject into the fields of the specified service instance.
     *
     * @param descriptor the dependency service descriptor
     * @param type the type of the dependency to be instantiated
     * @param value the instance of the service
     * @param context the class that requested the dependency
     *
     * @param <TService> the type of the service
     */
    private <TService> void injectService(
        @NotNull Service descriptor, @NotNull Class<TService> type, @NotNull TService value, @NotNull Class<?> context
    ) {
        // inject the dependencies for the instance's fields
        injectFields(type, value, context);

        // inject the implementations for custom annotations into the service instance
        injectProviders(type, value);

        // apply the registered hooks to the instance
        value = applyHooks(value, descriptor);

        // call each method of the service annotated with @AfterInitialized
        handleServiceInit(value, type);

        // call each method of the service annotated with custom annotations
        inspectMethods(value, type);
    }

    /**
     * Inspect the declared methods of the specified service instance, and invoke the registered inspector.
     *
     * @param instance the instance of the service type
     * @param type the class type of the service
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void inspectMethods(@NotNull Object instance, @NotNull Class<?> type) {
        // resolve each method of the implementing service class
        // we must check the top level class
        for (Method method : instance.getClass().getDeclaredMethods()) {
            // ignore static methods, as they are not a part of service instances
            if (Modifier.isStatic(method.getModifiers()))
                continue;

            // find the corresponding inspector for the annotations of the method
            inspectors.forEach((annotation, inspector) -> {
                // check if the method specifies a custom annotation
                Annotation annotationInstance = method.getDeclaredAnnotation(annotation);
                if (annotationInstance == null)
                    return;

                // call the inspector with the current method
                ServiceMethod info = new ServiceMethodImpl(type, instance, method);
                try {
                    ((MethodInspector) inspector).inspect(info, annotationInstance, this);
                } catch (Exception e) {
                    throw new ServiceInitializationException("Error while inspecting method " + method, e);
                }
            });
        }
    }

    /**
     * Handle post creation of a service and call each service method that is annotated with {@link AfterInitialized}.
     *
     * @param service the service instance to handle
     * @param type the class type of the service
     *
     * @param <TService> the type of the service
     *
     * @throws ServiceRuntimeException if an error occurs while invoking the service initialization method
     */
    private <TService> void handleServiceInit(
        @NotNull TService service, @NotNull Class<TService> type
    ) throws ServiceRuntimeException {
        // iterate over each method of the service class
        for (Method method : type.getDeclaredMethods()) {
            // skip methods that are not annotated with @AfterInitialized
            AfterInitialized init = method.getDeclaredAnnotation(AfterInitialized.class);
            if (init == null)
                continue;

            // make the method accessible for the dependency injector
            method.setAccessible(true);

            // register the method to be lazily invoked by the container
            if (init.lazy()) {
                lazyMethods.get().put(method, service);
                continue;
            }

            // invoke the method on the service instance
            try {
                method.invoke(service);
            } catch (InvocationTargetException | IllegalAccessException e) {
                throw new ServiceRuntimeException(
                    "Error whilst invoking initialization method `" + method.getName() + "` of service " +
                    type.getName(), e
                );
            }
        }
    }

    /**
     * Resolve the list of methods that should be called before the service is terminated and is removed from
     * the container.
     *
     * @param type the class type of the dependency
     * @return the list of methods that should be called before the service is terminated
     *
     * @param <TService> the type of the dependency
     */
    private <TService> @NotNull List<Method> resolveTerminators(@NotNull Class<TService> type) {
        List<Method> terminators = new ArrayList<>();
        // iterate over each method of the service class
        for (Method method : type.getDeclaredMethods()) {
            // skip methods that are not annotated with @BeforeTerminate
            if (!method.isAnnotationPresent(BeforeTerminate.class))
                continue;

            // register the method as a terminator
            method.setAccessible(true);
            terminators.add(method);
        }
        return terminators;
    }

    /**
     * Validate the specified properties of the service on service initialization
     *
     * @param service the service descriptor of the dependency
     * @param properties the specified properties to be passed to the factory
     *
     * @param <TProperties> the type of the properties that will be passed
     *
     * @throws InvalidServiceAccessException if the service is being accessed with invalid properties
     */
    private <TProperties> void validateProperties(
        @NotNull Service service, @Nullable TProperties properties
    ) throws InvalidServiceException {
        Class<? extends Factory<?, ?>> factoryType = service.factory();

        // return if the service has no factory specified
        if (factoryType == NoFactory.class) {
            // check if the service did not specify a factory, but factory properties were provided
            if (properties != null)
                throw new InvalidServiceAccessException(
                    "Service " + service + " does not have a factory, but factory properties were provided: " +
                    properties
                );

            return;
        }

        // resolve the required properties type from the service factory
        Class<?> propertiesType = getFactoryPropertiesType(factoryType);

        // return if the service factory has no properties specified
        if (propertiesType == NoProperties.class) {
            // check if the service factory required no properties, but properties were specified
            if (properties != null)
                throw new InvalidServiceAccessException(
                    "Service " + service + " factory " + factoryType.getName() +
                    " does not require properties, but were provided: " + properties
                );

            return;
        }

        // check if the service specified a factory, but no factory properties were provided
        if (properties == null)
            throw new InvalidServiceAccessException(
                "Service " + service + " factory " + factoryType.getName() +
                " requires properties, but none were provided"
            );

        // check if the specified properties does not match the required type defined in the service factory
        if (!propertiesType.isAssignableFrom(properties.getClass()))
            throw new InvalidServiceAccessException(
                "Service " + service + " factory " + factoryType.getName() + " requires properties of type " +
                propertiesType + ", but were given " + properties.getClass() + " of value " + properties
            );
    }

    /**
     * Resolve the type of the properties, that the service's factory has specified.
     *
     * @param factoryType the factory type of the service descriptor
     * @return the generic properties type of the factory
     *
     * @throws InvalidServiceAccessException if the factory type is invalid or the properties type cannot be resolved
     */
    private @NotNull Class<?> getFactoryPropertiesType(
        @NotNull Class<? extends Factory<?, ?>> factoryType
    ) throws InvalidServiceAccessException {
        // resolve the implementing interfaces of the factory type
        Type[] genericInterfaces = factoryType.getGenericInterfaces();
        if (genericInterfaces.length == 0)
            throw new InvalidServiceAccessException(
                "Factory " + factoryType.getName() + " requires generic types: <TService, TProperties>, but found none"
            );

        // TODO instead of assuming the factory implementation has `Factory` as its first element in the interface list
        //  loop through each type, and check which one actually is assignable from `Factory`
        Type genericInterface = genericInterfaces[0];

        // resolve the generic arguments of the factory type
        Type[] actualTypeArguments = getGenericTypes(factoryType, genericInterface);

        return (Class<?>) actualTypeArguments[1];
    }

    /**
     * Resolve the generic types of the specified type.
     *
     * @param factoryType the type of the factory
     * @param genericInterface the factory interface of the factory implementation type
     * @return the generic types of the factory interface
     *
     * @throws InvalidServiceAccessException if the factory type is invalid or the generic types cannot be resolved
     */
    private @NotNull Type @NotNull [] getGenericTypes(
        @NotNull Class<? extends Factory<?, ?>> factoryType, @NotNull Type genericInterface
    ) throws InvalidServiceAccessException {
        // check if the implemented factory type is not a parameterized type, however, this should never happen
        Type[] actualTypeArguments = getActualTypes(factoryType, genericInterface);
        // validate that the interface has two generic types: TService and TProperties, however, this should never happen
        if (actualTypeArguments.length != 2)
            throw new InvalidServiceAccessException(
                "Factory " + factoryType + " requires one actual type argument for TProperties, but found: " +
                actualTypeArguments.length
            );

        return actualTypeArguments;
    }

    /**
     * Resolve the actual generic types from the factory interface type.
     *
     * @param factoryType the type of the factory
     * @param genericInterface the factory interface of the factory implementation type
     * @return the actual generic types of the factory interface
     *
     * @throws InvalidServiceAccessException if the factory type is invalid or the generic types cannot be resolved
     */
    private @NotNull Type @NotNull [] getActualTypes(
        @NotNull Class<? extends Factory<?, ?>> factoryType, @NotNull Type genericInterface
    ) throws InvalidServiceAccessException {
        // check if the implemented factory type is not a parameterized type, however, this should never happen
        if (!(genericInterface instanceof ParameterizedType))
            throw new InvalidServiceAccessException(
                "Factory " + factoryType + " requires a parameterized type for TProperties, but found: " +
                genericInterface
            );

        // resolve the generic types of the implemented factory interface
        return ((ParameterizedType) genericInterface).getActualTypeArguments();
    }

    /**
     * Apply the registered hooks to the specified dependency value.
     *
     * @param value the dependency value to apply the hooks to
     * @return the modified dependency value
     * @param <T> the type of the dependency value
     */
    private <T> @NotNull T applyHooks(
        @NotNull T value, @NotNull Service service
    ) throws ServiceInitializationException {
        // loop through the registered dependency creation hooks
        for (Map.Entry<String, ContainerHook> entry : hooks.entrySet()) {
            // apply the hook for the value
            try {
                @SuppressWarnings("unchecked")
                T result = (T) entry.getValue().onDependencyCreated(value, service);
                value = result;
            } catch (Exception e) {
                throw new ServiceInitializationException(
                    "Hook " + entry.getKey() + " returned an invalid value for service " + value.getClass().getName()
                );
            }
        }

        return value;
    }

    /**
     * Create an instance of a service to be injected.
     *
     * @param fieldType the type of the field to be injected to
     * @param fieldName the name of the field to be injected to
     * @param targetClass the class that requested the dependency
     * @param inject the injection descriptor of the field
     * @param context the class that the dependency is being called from
     *
     * @return the created instance of the service to be injected
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private @NotNull Object createInjectionInstance(
        @NotNull Class<?> fieldType, @NotNull Type genericType, @NotNull String fieldName,
        @NotNull Class<?> targetClass, @NotNull Inject inject, @NotNull Class<?> context,
        @NotNull InjectionType injectionType
    ) {
        // throw an exception if lazy injection is applied to a non-class field target
        if (inject.lazy() && injectionType != InjectionType.CLASS_FIELD) {
            throw new InvalidServiceAccessException(
                "Lazy injection can only be applied for class fields, not for " + injectionType + ". If you want to " +
                "lazily inject a dependency for this target, use Ref<T> instead."
            );
        }

        // create a referenced access to the dependency to be lazily accessed
        if (Ref.class.isAssignableFrom(fieldType)) {
            Class<?> refType = resolveReferenceType(genericType);
            return (Ref<?>) () -> createInjectionInstance(
                refType, refType, fieldName, targetClass, inject, context, injectionType
            );
        }

        // retrieve the implementation type of the service, if it is specified
        Class<?> implementation = inject.implementation();

        // resolve the properties of the injection descriptor
        boolean hasToken = !inject.token().equals(Inject.NO_TOKEN);
        boolean hasProperties = !inject.properties().equals(Inject.NO_PROPERTIES);
        boolean hasImplementation = implementation != NoImplementation.class;

        // check if the injection descriptor has a token and properties specified at the same time
        if (hasToken && hasProperties)
            throw new ServiceInitializationException(
                "@Inject annotation cannot have a token and properties defined at the same time."
            );

        // resolve the container by its token from the container
        if (hasToken)
            return get(inject.token());

        // resolve the properties of the dependency to be passed to the factory
        Object properties = null;

        // check if the injection descriptor has string properties specified
        if (hasProperties)
            properties = inject.properties();

        // check if the injection descriptor has a property provider specified
        if (inject.provider() != NoPropertiesProvider.class) {
            // throw an exception if the field has both properties and a provider specified
            if (properties != null)
                throw new InvalidServiceException(
                    "Injection target " + injectionType.getName() + " " + fieldName + " of class " +
                    targetClass.getName() + " has both properties and a provider specified"
                );

            // create a new instance of the property provider and provide the properties for the factory
            PropertyProvider provider;
            try {
                provider = getConstructor(inject.provider()).newInstance();
            } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
                throw new ServiceInitializationException(
                    "Error while creating a new instance of the property provider " + inject.provider(), e
                );
            }
            properties = provider.provide(resolveDescriptor(fieldType, true), fieldType, context);
        }

        Class<?> dependencyType = fieldType;

        // check if the injection descriptor has an implementation specified
        if (hasImplementation) {
            if (!fieldType.isAssignableFrom(implementation))
                throw new ServiceInitializationException(
                    "Service " + injectionType.getName() +  " " + fieldName + " implementation " +
                    implementation.getName() + " does not implement the service type " + fieldType.getName()
                );
            // use the implementation of the service, if it is specified
            dependencyType = implementation;
        }

        // resolve and inject the dependency for the field
        return get(dependencyType, context, properties);
    }

    /**
     * Inject the lazy fields that are stored for the current dependency tree.
     */
    private void injectLazyFields() {
        // return if the dependency resolving tree is still being resolved
        if (!resolvingStack.get().isEmpty())
            return;

        // return and clean up if the lazy fields had been already injected
        // this prevents infinite loops when resolving circular dependencies
        if (injectingLazyFields.get()) {
            clearLazyFields();
            return;
        }

        // begin injecting the lazy fields
        injectingLazyFields.set(true);

        // resolve the lazy fields to be injected
        Map<Field, LazyFieldAccess> fields = lazyFields.get();

        // iterate over the lazy fields to be injected
        for (Map.Entry<Field, LazyFieldAccess> entry : fields.entrySet()) {
            // resolve the field and the access to the field
            Field field = entry.getKey();
            LazyFieldAccess access = entry.getValue();

            // inject the field into the instance
            injectField(
                field, access.getInstance(), field.getType(), field.getGenericType(), field.getName(), access.getType(),
                access.getDescriptor(), access.getContext()
            );
        }

        // clear the lazy fields after injecting them
        clearLazyFields();

        // end injecting the lazy fields
        injectingLazyFields.set(false);
    }

    /**
     * Clear the lazy fields that are stored for the current dependency tree.
     */
    private void clearLazyFields() {
        // clear any pending lazy fields that are stored in the thread-local map
        lazyFields.get().clear();
        // remove the thread-local map from the thread
        lazyFields.remove();
    }

    /**
     * Invoke the lazy methods that are stored for the current dependency tree.
     */
    private void invokeLazyMethods() {
        // return if the dependency resolving tree is still being resolved
        if (!resolvingStack.get().isEmpty())
            return;

        // return if currently injecting lazy fields
        // this prevents the lazy methods from being invoked before the lazy fields are injected
        if (injectingLazyFields.get())
            return;

        // return if the lazy methods have already been invoked
        // this prevents infinite loops when resolving circular dependencies
        if (invokingLazyMethods.get()) {
            clearLazyMethods();
            return;
        }

        // begin invoking the lazy methods
        invokingLazyMethods.set(true);

        // resolve the lazy methods to be invoked
        Map<Method, Object> methods = lazyMethods.get();

        // iterate over the lazy methods to be invoked
        for (Map.Entry<Method, Object> entry : methods.entrySet()) {
            // resolve the method and the instance to invoke the method on
            Method method = entry.getKey();
            Object instance = entry.getValue();

            // invoke the method on the instance
            try {
                method.invoke(instance);
            } catch (InvocationTargetException | IllegalAccessException e) {
                throw new ServiceInitializationException(
                    "Error whilst invoking initialization method `" + method.getName() + "` of service " +
                    instance.getClass().getName(), e
                );
            }
        }

        // clear the lazy methods after invoking them
        clearLazyMethods();

        // end invoking the lazy methods
        invokingLazyMethods.set(false);
    }

    /**
     * Clear the lazy methods that are stored for the current dependency tree.
     */
    private void clearLazyMethods() {
        // clear any pending lazy methods that are stored in the thread-local map
        lazyMethods.get().clear();
        // remove the thread-local map from the thread
        lazyMethods.remove();
    }

    /**
     * Inject the fields of the specified instance.
     *
     * @param clazz the class of the instance
     * @param instance the instance to inject the fields of
     * @param context the class that requested the dependency
     *
     * @param <T> the type of the instance
     */
    private <T> void injectFields(
        @NotNull Class<? extends T> clazz, @NotNull T instance, @NotNull Class<?> context
    ) throws ServiceInitializationException {
        // loop through the fields of the class
        for (Field field : clazz.getDeclaredFields()) {
            // resolve the injection descriptor of the field
            Inject inject = field.getAnnotation(Inject.class);
            if (inject == null)
                continue;

            // make sure the field is accessible for the dependency injector
            field.setAccessible(true);

            // register the field in the lazy fields map, if lazy injection is applied
            if (inject.lazy() && !injectingLazyFields.get()) {
                // in order to account for circular dependencies, we need to register the field on the first pass
                lazyFields.get().computeIfAbsent(field, k -> new LazyFieldAccess(
                    instance, clazz, inject, context
                ));
                continue;
            }

            // inject the field into the instance
            injectField(
                field, instance, field.getType(), field.getGenericType(), field.getName(), clazz, inject, context
            );
        }
    }

    /**
     * Inject the specified field into the instance of the service.
     *
     * @param field the field to be injected
     * @param instance the instance of the service
     * @param fieldType the type of the field
     * @param genericType the generic type of the field
     * @param fieldName the name of the field
     * @param targetClass the class that requested the dependency
     * @param inject the injection descriptor of the field
     * @param context the class that the dependency is being called from
     */
    private void injectField(
        @NotNull Field field, @NotNull Object instance, @NotNull Class<?> fieldType, @NotNull Type genericType,
        @NotNull String fieldName, @NotNull Class<?> targetClass, @NotNull Inject inject, @NotNull Class<?> context
    ) {
        // create an instance of the service to be injected
        Object injection = createInjectionInstance(
            fieldType, genericType, fieldName, targetClass, inject, context,
            InjectionType.CLASS_FIELD
        );

        // try to inject the instance of the service into the field
        try {
            field.set(instance, injection);
        } catch (IllegalAccessException e) {
            throw new ServiceInitializationException(
                "Error while injecting dependency " + injection.getClass().getName() + " into field " +
                    field.getName() + " of class " + targetClass.getName(), e
            );
        }
    }

    /**
     * Provide an implementation for a service from a registered custom annotation.
     *
     * @param annotations the annotations of the field
     * @param target the class that requested the dependency
     * @param handle the type of the injection and information of the requesting service
     *
     * @return the implementation of the annotation
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private @Nullable Object provideAnnotation(
        @NotNull Annotation @NotNull [] annotations, @NotNull Class<?> target, @NotNull InjectionHandle handle
    ) {
        // iterate over the annotations of the field
        for (Annotation annotation : annotations) {
            // skip annotations that are not registered in the container
            AnnotationProvider provider = providers.get(annotation.annotationType());
            if (provider == null)
                continue;

            // provide the implementation of the annotation
            try {
                return provider.provide(target, annotation, this, handle);
            } catch (Exception e) {
                throw new ServiceInitializationException(
                    "Error while providing annotation " + annotation.annotationType().getName() + " for " +
                    target.getName(), e
                );
            }
        }

        // field does not have a custom annotation provider, return null
        return null;
    }

    /**
     * Inject implementations for custom annotations into the specified service instance.
     *
     * @param type the class type of the service
     * @param instance the instance of the service
     *
     * @param <TService> the type of the service
     */
    private <TService> void injectProviders(
        @NotNull Class<TService> type, @NotNull TService instance
    ) throws ServiceInitializationException {
        // iterate over each field of the service class
        for (Field field : type.getDeclaredFields()) {
            // resolve the implementation of the service from the provider
            Object provide = provideAnnotation(
                field.getAnnotations(), field.getType(), new FieldInjectionHandle(type, instance, field)
            );
            // skip the field if the provider did not provide an implementation
            if (provide == null)
                continue;

            // make the field accessible for the dependency injector
            field.setAccessible(true);

            // inject the implementation of the service into the field
            try {
                field.set(instance, provide);
            } catch (IllegalAccessException e) {
                throw new ServiceInitializationException(
                    "Error while injecting into field " + field.getName() + " of class " + type.getName(), e
                );
            }
        }
    }

    /**
     * Create an instance of the specified service and inject the required dependencies in the constructor.
     *
     * @param type the type of the service
     * @param context the class context that requested the dependency
     * @return a new instance of the service type
     * @param <T> the type of the service
     *
     * @throws ServiceInitializationException if an error occurs while instantiating the service type
     */
    private <T> @NotNull T createInstanceWithDependencies(
        @NotNull Class<T> type, @NotNull Class<?> context
    ) throws ServiceInitializationException {
        // get the first constructor of the class
        Constructor<T> constructor = getConstructor(type);

        // create the arguments of the constructor call
        Class<?>[] types = constructor.getParameterTypes();
        Type[] genericTypes = constructor.getGenericParameterTypes();
        int parameterCount = constructor.getParameterCount();
        // the call arguments are initially `null`, as we have no proper way of resolving non-service-based parameters
        Object[] args = new Object[parameterCount];

        // loop through the constructor parameters
        for (int i = 0; i < parameterCount; i++) {
            // retrieve the metadata of the constructor parameter
            Parameter parameter = constructor.getParameters()[i];
            Class<?> paramType = types[i];
            Type genericType = genericTypes[i];
            String paramName = parameter.getName();

            // try to resolve the annotation list of the constructor parameters
            // sadly, this can fail, due to Java not properly handling this, such as returning an empty array
            // for that reason, I'm sticking with this workaround, and warning developers of this issue
            Annotation[] paramAnnotations = null;
            try {
                paramAnnotations = parameter.getDeclaredAnnotations();
            } catch (IndexOutOfBoundsException ignored) {
                System.err.println(
                    "Java Reflection API was unable to resolve the parameter annotations of constructor " +
                    constructor.getName() + " of class " + type.getName() + ". This can happen, when using " +
                    "non static inner classes, and the parameter annotations are not available. This may change " +
                    "the behaviour of the dependency injector, and may lead to unexpected results."
                );
            }

            // try to fall back to the default service resolving, if the parameter annotations are not available
            if (paramAnnotations == null) {
                if (paramType.isAnnotationPresent(Service.class))
                    args[i] = get(paramType, context);
                continue;
            }

            // handle custom descriptor injection
            Inject inject = parameter.getAnnotation(Inject.class);
            if (inject != null) {
                args[i] = createInjectionInstance(
                    paramType, genericType, paramName, type, inject, context,
                    InjectionType.CONSTRUCTOR_PARAMETER
                );
                continue;
            }

            // handle custom annotation injection
            Object provide = provideAnnotation(
                paramAnnotations, paramType, new ConstructorInjectionHandle(type)
            );
            if (provide != null)
                args[i] = provide;

            // handle default service resolving
            else if (paramType.isAnnotationPresent(Service.class))
                args[i] = get(paramType, context);
        }

        // create the instance with the resolved service arguments
        try {
            return constructor.newInstance(args);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new ServiceInitializationException(
                "Error while creating a new instance of service " + type.getName(), e
            );
        }
    }

    /**
     * Instantiate the factory of the specified class type.
     *
     * @param type the class type of the factory
     * @return the factory instance of the specified type
     * @param <TService> the service type of the factory
     *
     * @throws ServiceInitializationException if an error occurs while creating the factory instance
     */
    @SuppressWarnings("unchecked")
    private <TService, TProperties> @Nullable Factory<TService, TProperties> createFactory(
        @NotNull Class<? extends Factory<?, ?>> type
    ) throws ServiceInitializationException {
        // return null if the factory was not specified
        if (type == NoFactory.class)
            return null;

        // resolve the constructor of the factory
        Constructor<? extends Factory<?, ?>> constructor = getConstructor(type);

        // create a new instance of the factory
        try {
            return (Factory<TService, TProperties>) constructor.newInstance();
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new ServiceInitializationException(
                "Error while creating a new instance of factory " + type.getName(), e
            );
        }
    }

    /**
     * Resolve the constructor of the specified class type, that the dependency injector should use
     * to instantiate the class type with.
     *
     * @param type the class of the type
     * @return the constructor of the class type
     *
     * @param <T> the type of the class
     */
    @SuppressWarnings("unchecked")
    private <T> @NotNull Constructor<T> getConstructor(@NotNull Class<T> type) throws InvalidServiceException {
        Constructor<T> constructor = null;

        // if the class has only one constructor, use it to create the instance
        Constructor<T>[] constructors = (Constructor<T>[]) type.getDeclaredConstructors();
        if (constructors.length == 1)
            constructor = constructors[0];

        // if the class has multiple constructors, use the one annotated with @ConstructWith
        else {
            // check for each constructor declared by the service's class
            for (Constructor<T> test : constructors) {
                if (test.isAnnotationPresent(ConstructWith.class)) {
                    constructor = test;
                    break;
                }
            }
            // check if no constructor were indicated, for the dependency injector to construct with
            if (constructor == null)
                throw new InvalidServiceException(
                    "Class " + type.getName() + " has multiple constructors, but none of them is annotated with " +
                    "@ConstructWith, therefore the dependency injector cannot decide, which one to use."
                );
        }

        constructor.setAccessible(true);
        return constructor;
    }

    /**
     * Check if the caller class context has permission to access the specified dependency type.
     *
     * @param dependency the type of the dependency
     * @param visibility the visibility of the dependency
     * @param context the class context that tries to access the dependency
     */
    private void checkAccess(
        @NotNull Class<?> dependency, @NotNull ServiceVisibility visibility, @NotNull Class<?> context
    ) throws InvalidServiceAccessException {
        // check if the accessing context does not match the class the dependency was instantiated for
        if (visibility == ServiceVisibility.PRIVATE && !dependency.equals(context))
            throw new InvalidServiceAccessException(
                "Class " + context.getName() + " does not have permission to access private service " +
                dependency.getName()
            );

        // check if the accessing context is not a subclass of the class the dependency was instantiated for
        else if (visibility == ServiceVisibility.PROTECTED && !dependency.isAssignableFrom(context))
            throw new InvalidServiceAccessException(
                "Class " + context.getName() + " does not have permission to access protected service " +
                dependency.getName()
            );

        // check if the accessing context does not match the context the dependency was instantiated for
        else if (
            visibility == ServiceVisibility.CONTEXT &&
            !dependency.getClassLoader().equals(context.getClassLoader())
        ) {
            throw new InvalidServiceAccessException(
                "Class " + context.getName() + " does not have permission to access local service " +
                dependency.getName()
            );
        }
    }

    /**
     * Retrieve a global value from the container for the specified token.
     *
     * @param token the unique identifier of the value
     * @return the value of the desired token
     *
     * @throws UnknownDependencyException if the value is not found, invalid, or the caller context
     */
    @Override
    public <T> @NotNull T get(@NotNull String token) {
        // check if the value has not been stored yet
        if (!has(token))
            throw new UnknownDependencyException("Unknown dependency: " + token);

        // retrieve the value from the container
        @SuppressWarnings("unchecked")
        T value = (T) values.get(token);
        return value;
    }

    /**
     * Inject into the fields of a service that was instantiated without DiVine.
     *
     * @param service the instance of the service to inject into
     * @return the service instance
     *
     * @param <T> the type of the service
     */
    @Override
    public <T> @NotNull T injectInto(@NotNull T service) {
        injectFields(service.getClass(), service, service.getClass());
        return service;
    }

    /**
     * Manually update the value of the specified dependency type in the container cache.
     *
     * @param type the class type of the dependency
     * @param dependency the new instance of the dependency
     * @param <T> the type of the dependency
     */
    @Override
    public <T> void set(@NotNull Class<T> type, @NotNull T dependency) {
        try {
            set(type, dependency, Contexts.getCallerClass(Thread.currentThread().getStackTrace()));
        } catch (ClassNotFoundException e) {
            set(type, dependency, Container.class);
        }
    }

    /**
     * Manually update the value of the specified dependency type in the container cache.
     *
     * @param type the class type of the dependency
     * @param dependency the new instance of the dependency
     * @param context the class that the dependency was instantiated for
     *
     * @throws InvalidServiceException if the service descriptor is invalid, transient, or the access is denied
     */
    @Override
    public <T> void set(@NotNull Class<T> type, @NotNull T dependency, Class<?> context) {
        // validate that the service type annotates the service descriptor annotation
        Service service = type.getAnnotation(Service.class);
        if (service == null)
            throw new InvalidServiceException("Class " + type.getName() + " is not a service");

        // use the root container to cache the singleton service
        ServiceScope scope = service.scope();
        if (scope == ServiceScope.SINGLETON && rootContainer != null)
            rootContainer.set(type, dependency, context);

        // do not set the instance of a transient service, as it is created every time it is accessed
        else if (scope == ServiceScope.TRANSIENT)
            throw new InvalidServiceException("Cannot set transient service " + type.getName() + " in the container");

        // validate that the caller class has access to the service type
        checkAccess(type, service.visibility(), context);

        // call termination hooks before the service is replaced
        unset(type);

        // cache the service instance in the container
        dependencies.put(type, new CachedDependency<>(dependency, service, context, resolveTerminators(type)));
    }

    /**
     * Update the value of the specified token in the container cache.
     *
     * @param token the unique identifier of the value
     * @param value the new value of the token
     */
    @Override
    public <T> void set(@NotNull String token, @NotNull T value) {
        values.put(token, value);
    }

    /**
     * Check if the container has a dependency instance for the specified class type.
     *
     * @param type the class type of the dependency
     * @return true if the container has the dependency, otherwise false
     */
    @Override
    public <T> boolean has(@NotNull Class<T> type) {
        return dependencies.containsKey(type);
    }

    /**
     * Check if the container has a global value for the specified token.
     *
     * @param token the unique identifier of the value
     * @return true if the container has the value, otherwise false
     */
    @Override
    public boolean has(@NotNull String token) {
        return values.containsKey(token);
    }

    /**
     * Remove the dependency instance from the container cache for the specified class type.
     *
     * @param type the class type of the dependency
     */
    @Override
    public <T> void unset(@NotNull Class<T> type) {
        // TODO check access to the container
        CachedDependency<?> dependency = dependencies.remove(type);
        if (dependency == null)
            return;

        handleTerminate(type.cast(dependency.value()), dependency.terminators());
    }

    /**
     * Remove the dependency instance from the container cache for the specified class type.
     *
     * @param type the class type of the dependency
     * @param callback the callback that will be called with the dependency instance
     * @param <T> the type of the dependency
     */
    @Override
    public <T> void unset(@NotNull Class<T> type, @NotNull Consumer<T> callback) {
        CachedDependency<?> dependency = dependencies.remove(type);
        if (dependency == null)
            return;

        T value = type.cast(dependency.value());
        callback.accept(value);
        handleTerminate(value, dependency.terminators());
    }

    /**
     * Call each method of the service class that is annotated with {@link BeforeTerminate}.
     *
     * @param value the service instance to handle
     * @param methods the list of methods that should be called before the service is terminated
     * @param <TService> the type of the service
     */
    private <TService> void handleTerminate(
        @NotNull TService value, @NotNull List<Method> methods
    ) throws ServiceRuntimeException {
        for (Method method : methods) {
            try {
                method.invoke(value);
            } catch (InvocationTargetException | IllegalAccessException e) {
                throw new ServiceRuntimeException(
                    "Error whilst invoking termination method `" + method.getName() + "` of service " +
                    value.getClass().getName(), e
                );
            }
        }
    }

    /**
     * Remove the global value from the container cache for the specified token.
     *
     * @param token the unique identifier of the value
     */
    @Override
    public void unset(@NotNull String token) {
        values.remove(token);
    }

    /**
     * Reset the container instance to its initial state.
     * <p>
     * Invalidate the cache for all the registered dependencies and values.
     */
    @Override
    public void reset() {
        // TODO check access to the container

        // call termination hooks before the container is cleared
        for (Class<?> type : dependencies.keySet())
            unset(type);

        dependencies.clear();
        values.clear();
    }

    /**
     * Retrieve the list of the containers registered locally in the container.
     *
     * @return local container instances
     */
    public @NotNull Set<ContainerRegistry> getLocalContainers() {
        // resolve the containers from the local container
        Set<ContainerRegistry> local = new HashSet<>();
        local.add(this);

        // recursively resolve the containers from the child containers
        for (DefaultContainerImpl child : containers.values())
            local.addAll(child.getLocalContainers());

        return local;
    }

    /**
     * Retrieve the list of all the registered dependencies in the container tree.
     *
     * @return each instance of the registered dependencies in the container tree
     */
    public @NotNull List<CachedDependency<?>> getTotalDependencies() {
        // make sure to start resolving the tree from the root container
        if (rootContainer instanceof DefaultContainerImpl)
            return ((DefaultContainerImpl) rootContainer).getTotalDependencies();

        // recursively resolve the dependencies from the child containers
        return getLocalDependencies();
    }

    /**
     * Retrieve the list of the dependencies registered locally in the container.
     *
     * @return local container dependencies
     */
    public @NotNull List<CachedDependency<?>> getLocalDependencies() {
        // resolve the dependencies from the local container
        List<CachedDependency<?>> local = new ArrayList<>(dependencies.values());

        // recursively resolve the dependencies from the child containers
        for (DefaultContainerImpl child : containers.values())
            local.addAll(child.getLocalDependencies());

        return local;
    }

    /**
     * Retrieve the string representation of this container instance.
     *
     * @return the container instance debug information
     */
    @Override
    public String toString() {
        return "DefaultContainerImpl{" +
            "name='" + name + '\'' +
            ", rootContainer=" + (rootContainer != null ? rootContainer.getName() : null) +
            ", dependencies=" + dependencies.size() +
            ", values=" + values.size() +
            ", containers=" + containers.size() +
            ", hooks=" + hooks.size() +
            '}';
    }

    /**
     * Retrieve the json representation of this container registry.
     *
     * @return the container data exported as json
     */
    @Override
    public @NotNull JsonObject export() {
        JsonObject json = new JsonObject();

        json.addProperty("name", name);

        JsonArray dependencies = new JsonArray();
        for (CachedDependency<?> dependency : this.dependencies.values())
            dependencies.add(dependency.export());
        json.add("dependencies", dependencies);

        JsonObject values = new JsonObject();
        for (Map.Entry<String, Object> entry : this.values.entrySet())
            values.addProperty(entry.getKey(), entry.getValue().toString());
        json.add("values", values);

        JsonObject hooks = new JsonObject();
        for (Map.Entry<String, ContainerHook> entry : this.hooks.entrySet())
            hooks.addProperty(entry.getKey(), entry.getValue().toString());
        json.add("hooks", hooks);

        JsonArray containers = new JsonArray();
        for (DefaultContainerImpl container : this.containers.values())
            containers.add(container.export());
        json.add("containers", containers);

        return json;
    }
}
