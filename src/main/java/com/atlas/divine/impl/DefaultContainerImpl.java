package com.atlas.divine.impl;

import com.atlas.divine.*;
import com.atlas.divine.tree.cache.ContainerHook;
import com.atlas.divine.tree.cache.Dependency;
import com.atlas.divine.descriptor.generic.*;
import com.atlas.divine.exception.UnknownDependencyException;
import com.atlas.divine.runtime.lifecycle.AfterInitialized;
import com.atlas.divine.runtime.lifecycle.BeforeTerminate;
import com.atlas.divine.descriptor.property.PropertyProvider;
import com.atlas.divine.runtime.context.Security;
import com.atlas.divine.descriptor.factory.Factory;
import com.atlas.divine.descriptor.factory.NoFactory;
import com.atlas.divine.descriptor.implementation.NoImplementation;
import com.atlas.divine.descriptor.property.NoProperties;
import com.atlas.divine.descriptor.property.NoPropertiesProvider;
import com.atlas.divine.tree.ContainerRegistry;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
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
    private final @NotNull Map<@NotNull Class<?>, @NotNull CachedDependency<?>> dependencies = new ConcurrentHashMap<>();

    /**
     * The map of the globally registered values in the container.
     */
    private final @NotNull Map<@NotNull String, @NotNull Object> values = new ConcurrentHashMap<>();

    /**
     * The map of the sub-containers registered to this container.
     */
    private final @NotNull Map<@NotNull String, @NotNull DefaultContainerImpl> containers = new ConcurrentHashMap<>();

    /**
     * The map of the hooks registered to this container that can modify dependencies on creation.
     */
    private final @NotNull Map<@NotNull String, @NotNull ContainerHook> hooks = new ConcurrentHashMap<>();

    /**
     * The root container of the container hierarchy.
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
            container = new DefaultContainerImpl(rootContainer != null ? rootContainer : this);
            containers.put(name, container);
        }
        return container;
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
     * Retrieve an instance from the container for the specified class type. Based on the service descriptor,
     * a dependency instance may be retrieved from the container cache, or a new instance is created.
     *
     * @param type the class type of the dependency
     * @return the instance of the desired dependency type
     *
     * @throws UnknownDependencyException if the dependency is not found, invalid, or the caller context
     * does not have permission to access the dependency
     */
    @Override
    @SneakyThrows
    public <T> @NotNull T get(@NotNull Class<T> type) {
        return get(type, Security.getCallerClass(Thread.currentThread().getStackTrace()));
    }

    /**
     * Retrieve an instance from the container for the specified class type. Based on the service descriptor,
     * a dependency instance may be retrieved from the container cache, or a new instance is created.
     *
     * @param type the class type of the dependency
     * @param context the caller class that the container is being called from
     * @return the instance of the desired dependency type
     *
     * @throws UnknownDependencyException if the dependency is not found, invalid, or the caller context
     * does not have permission to access the dependency
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
     * @throws UnknownDependencyException if the dependency is not found, invalid, or the caller context
     * does not have permission to access the dependency
     */
    @Override
    @SneakyThrows
    public <TService, TProperties> @NotNull TService get(
        @NotNull Class<TService> type, @Nullable TProperties properties
    ) {
        return get(type, Security.getCallerClass(Thread.currentThread().getStackTrace()), properties);
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
     * @throws UnknownDependencyException if the dependency is not found, invalid, or the caller context
     *  does not have permission to access the dependency
     */
    @Override
    public <TService, TProperties> @NotNull TService get(
        @NotNull Class<TService> type, @NotNull Class<?> context, @Nullable TProperties properties
    ) {
        // resolve the service descriptor of the dependency type
        Service service = resolveDescriptor(type);

        // resolve the dependency from the root container if it has a singleton scope
        ServiceScope scope = service.scope();
        if (scope == ServiceScope.SINGLETON && rootContainer != null)
            return rootContainer.get(type, context);
        // if the root container is null, that means that the current container is the root

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
     */
    @Override
    public <TService, TImplementation extends TService> @NotNull TService implement(
        @NotNull Class<TService> type, @NotNull Class<TImplementation> implementationType
    ) {
        TImplementation implementation = Container.get(implementationType);
        Container.set(type, implementation);
        return implementation;
    }

    /**
     * Resolve the service descriptor of the specified dependency type.
     *
     * @param type the class type of the dependency
     * @return the service descriptor of the dependency type
     * @param <T> the type of the dependency
     */
    private <T> @NotNull Service resolveDescriptor(@NotNull Class<T> type) {
        // validate that the service type annotates the service descriptor annotation
        Service service = type.getAnnotation(Service.class);
        if (service == null)
            throw new UnknownDependencyException("Class " + type.getName() + " is not a service");

        // make sure not to use annotation types for dependencies
        if (type.isAnnotation())
            throw new UnknownDependencyException("Annotation type " + type.getName() + " cannot be a service");

        // validate that the service type has a factory specified if it is an enum
        if (type.isEnum() && service.factory() == NoFactory.class) {
            if (service.scope() == ServiceScope.TRANSIENT)
                throw new UnknownDependencyException(
                    "Transient enum service type " + type.getName() + " must have a factory specified"
                );

            if (!has(type))
                throw new UnknownDependencyException(
                    "Enum service type " + type.getName() + " must have a factory specified in the service " +
                    "descriptor, or it must be already initialized in the container"
                );
        }

        // validate that the service type has a factory or an implementation specified if it is an interface
        if (
            type.isInterface() && service.factory() == NoFactory.class &&
            service.implementation() == NoImplementation.class && !has(type)
        )
            throw new UnknownDependencyException(
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
     * @param service the dependency service descriptor
     * @param context the class that requested the dependency
     * @return a new instance of the service type
     *
     * @param <TService> the type of the dependency
     * @param <TProperties> the type of the properties to pass to the factory
     */
    @SuppressWarnings("unchecked")
    private <TService, TProperties> TService createInstance(
        @NotNull Class<TService> type, @NotNull Service service, @NotNull Class<?> context,
        @Nullable TProperties properties
    ) {
        TService value;
        Class<?> implementation = service.implementation();

        // validate that the properties match the factory of the service
        validateProperties(service, properties);

        // resolve the factory from the service descriptor
        // use the factory to instantiate the service, if it is specified
        Factory<TService, TProperties> factory = createFactory(service.factory());
        if (factory != null)
            value = factory.create(service, type, context, properties);
        // use the implementation of the service, if it is specified
        else if (implementation != NoImplementation.class) {
            if (!type.isAssignableFrom(implementation))
                throw new UnknownDependencyException(
                    "Service " + service + " implementation " + implementation.getName() +
                    " does not implement the service type " + type.getName()
                );
            type = (Class<TService>) implementation;
            value = createInstanceWithDependencies(type, context);
        }
        // let the dependency injector instantiate the service and resolve its constructor dependencies
        else
            value = createInstanceWithDependencies(type, context);

        // inject the dependencies for the instance's fields
        injectFields(type, value, context);

        // apply the registered hooks to the instance
        value = applyHooks(value, service);

        // call each method of the service annotated with @AfterInitialized
        handleServiceInit(value, type);

        return value;
    }

    /**
     * Handle post creation of a service and call each service method that is annotated with {@link AfterInitialized}.
     *
     * @param service the service instance to handle
     * @param type the class type of the service
     * @param <TService> the type of the service
     */
    @SneakyThrows
    private <TService> void handleServiceInit(@NotNull TService service, @NotNull Class<TService> type) {
        // iterate over each method of the service class
        for (Method method : type.getDeclaredMethods()) {
            // skip methods that are not annotated with @AfterInitialized
            if (!method.isAnnotationPresent(AfterInitialized.class))
                continue;

            // make the method accessible for the dependency injector
            method.setAccessible(true);

            // invoke the method on the service instance
            method.invoke(service);
        }
    }

    /**
     * Resolve the list of methods that should be called before the service is terminated and is removed from
     * the container.
     *
     * @param type the class type of the dependency
     * @return the list of methods that should be called before the service is terminated
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
     * @param <TProperties> the type of the properties that will be passed
     */
    private <TProperties> void validateProperties(@NotNull Service service, @Nullable TProperties properties) {
        Class<? extends Factory<?, ?>> factoryType = service.factory();

        // return if the service has no factory specified
        if (factoryType == NoFactory.class) {
            // check if the service did not specify a factory, but factory properties were provided
            if (properties != null)
                throw new UnknownDependencyException(
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
                throw new UnknownDependencyException(
                    "Service " + service + " factory " + factoryType.getName() +
                    " does not require properties, but were provided: " + properties
                );

            return;
        }

        // check if the service specified a factory, but no factory properties were provided
        if (properties == null)
            throw new UnknownDependencyException(
                "Service " + service + " factory " + factoryType.getName() +
                " requires properties, but none were provided"
            );

        // check if the specified properties does not match the required type defined in the service factory
        if (!propertiesType.isAssignableFrom(properties.getClass()))
            throw new UnknownDependencyException(
                "Service " + service + " factory " + factoryType.getName() + " requires properties of type " +
                propertiesType + ", but were given " + properties.getClass() + " of value " + properties
            );
    }

    /**
     * Resolve the type of the properties, that the service's factory has specified.
     *
     * @param factoryType the factory type of the service descriptor
     * @return the generic properties type of the factory
     */
    private @NotNull Class<?> getFactoryPropertiesType(@NotNull Class<? extends Factory<?, ?>> factoryType) {
        // resolve the implementing interfaces of the factory type
        Type[] genericInterfaces = factoryType.getGenericInterfaces();
        if (genericInterfaces.length == 0)
            throw new UnknownDependencyException(
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
     */
    private @NotNull Type @NotNull [] getGenericTypes(
        @NotNull Class<? extends Factory<?, ?>> factoryType, Type genericInterface
    ) {
        // check if the implemented factory type is not a parameterized type, however, this should never happen
        Type[] actualTypeArguments = getActualTypes(factoryType, genericInterface);
        // validate that the interface has two generic types: TService and TProperties, however, this should never happen
        if (actualTypeArguments.length != 2)
            throw new UnknownDependencyException(
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
     */
    private @NotNull Type @NotNull [] getActualTypes(
        @NotNull Class<? extends Factory<?, ?>> factoryType, Type genericInterface
    ) {
        // check if the implemented factory type is not a parameterized type, however, this should never happen
        if (!(genericInterface instanceof ParameterizedType))
            throw new UnknownDependencyException(
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
    private <T> T applyHooks(@NotNull T value, @NotNull Service service) {
        // loop through the registered dependency creation hooks
        for (Map.Entry<String, ContainerHook> entry : hooks.entrySet()) {
            // apply the hook for the value
            try {
                @SuppressWarnings("unchecked")
                T result = (T) entry.getValue().onDependencyCreated(value, service);
                value = result;
            } catch (Exception e) {
                throw new IllegalStateException(
                    "Hook " + entry.getKey() + " returned an invalid value for service " + value.getClass().getName()
                );
            }
        }

        return value;
    }

    /**
     * Inject the fields of the specified instance.
     *
     * @param clazz the class of the instance
     * @param instance the instance to inject the fields of
     * @param context the class that requested the dependency
     * @param <T> the type of the instance
     */
    @SneakyThrows
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private <T> void injectFields(@NotNull Class<T> clazz, @NotNull T instance, @NotNull Class<?> context) {
        // loop through the fields of the class
        for (Field field : clazz.getDeclaredFields()) {
            // make sure the field is accessible for the dependency injector
            field.setAccessible(true);

            // retrieve the type metadata of the field
            Class<?> fieldType = field.getType();

            // resolve the injection descriptor of the field
            Inject inject = field.getAnnotation(Inject.class);
            if (inject == null)
                continue;

            Class<?> implementation = inject.implementation();

            // resolve the properties of the injection descriptor
            boolean hasToken = !inject.token().equals(Inject.NO_TOKEN);
            boolean hasProperties = !inject.properties().equals(Inject.NO_PROPERTIES);
            boolean hasImplementation = implementation != NoImplementation.class;

            // check if the injection descriptor has a token and properties specified at the same time
            if (hasToken && hasProperties)
                throw new UnknownDependencyException(
                    "@Inject annotation cannot have a token and properties defined at the same time."
                );

            // resolve the container by its token from the container
            if (hasToken) {
                field.set(instance, get(inject.token()));
                break;
            }

            // resolve the properties of the dependency to be passed to the factory
            Object properties = null;

            // check if the injection descriptor has string properties specified
            if (hasProperties)
                properties = inject.properties();

            // check if the injection descriptor has a property provider specified
            if (inject.provider() != NoPropertiesProvider.class) {
                // throw an exception if the field has both properties and a provider specified
                if (properties != null)
                    throw new UnknownDependencyException(
                        "Field " + field.getName() + " of class " + clazz.getName() +
                        " has both properties and a provider specified"
                    );

                // create a new instance of the property provider and provide the properties for the factory
                PropertyProvider provider = getConstructor(inject.provider()).newInstance();
                properties = provider.provide(resolveDescriptor(fieldType), fieldType, context);
            }

            Class<?> dependencyType = fieldType;

            // check if the injection descriptor has an implementation specified
            if (hasImplementation) {
                if (!fieldType.isAssignableFrom(implementation))
                    throw new UnknownDependencyException(
                        "Service field " + field + " implementation " + implementation.getName() +
                        " does not implement the service type " + fieldType.getName()
                    );
                // use the implementation of the service, if it is specified
                dependencyType = implementation;
            }

            // resolve and inject the dependency for the field
            field.set(instance, get(dependencyType, context, properties));
        }
    }

    /**
     * Create an instance of the specified service and inject the required dependencies in the constructor.
     *
     * @param type the type of the service
     * @param context the class context that requested the dependency
     * @return a new instance of the service type
     * @param <T> the type of the service
     */
    @SneakyThrows
    private <T> @NotNull T createInstanceWithDependencies(@NotNull Class<T> type, @NotNull Class<?> context) {
        // get the first constructor of the class
        Constructor<T> constructor = getConstructor(type);

        // create the arguments of the constructor call
        Class<?>[] types = constructor.getParameterTypes();
        int parameterCount = constructor.getParameterCount();
        Object[] args = new Object[parameterCount];

        // loop through the constructor parameters
        for (int i = 0; i < parameterCount; i++) {
            // check if the parameter is not annotated with @Service
            Class<?> paramType = types[i];
            if (!paramType.isAnnotationPresent(Service.class))
                continue;

            // get the dependency from the container
            args[i] = get(paramType, context);
        }

        // create the instance with the resolved service arguments
        return constructor.newInstance(args);
    }

    /**
     * Instantiate the factory of the specified class type.
     *
     * @param type the class type of the factory
     * @return the factory instance of the specified type
     * @param <TService> the service type of the factory
     */
    @SneakyThrows
    @SuppressWarnings("unchecked")
    private <TService, TProperties> @Nullable Factory<TService, TProperties> createFactory(
        @NotNull Class<? extends Factory<?, ?>> type
    ) {
        // return null if the factory was not specified
        if (type == NoFactory.class)
            return null;

        // resolve the constructor of the factory
        Constructor<? extends Factory<?, ?>> constructor = getConstructor(type);

        // create a new instance of the factory
        return (Factory<TService, TProperties>) constructor.newInstance();
    }

    /**
     * Resolve the constructor of the specified class type, that the dependency injector should use
     * to instantiate the class type with.
     *
     * @param type the class of the type
     * @return the constructor of the class type
     * @param <T> the type of the class
     */
    @SuppressWarnings("unchecked")
    private <T> Constructor<T> getConstructor(@NotNull Class<T> type) {
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
            // check if no constructor were indicated, for TypeDI to construct with
            if (constructor == null)
                throw new IllegalStateException(
                    "Class " + type.getName() + " has multiple constructors, but none of them is annotated with " +
                    "@ConstructWith, therefore TypeDI cannot decide, which one to use."
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
    ) {
        // check if the accessing context does not match the class the dependency was instantiated for
        if (visibility == ServiceVisibility.PRIVATE && !dependency.equals(context))
            throw new UnknownDependencyException(
                "Class " + context.getName() + " does not have permission to access private service " +
                dependency.getName()
            );

        // check if the accessing context is not a subclass of the class the dependency was instantiated for
        else if (visibility == ServiceVisibility.PROTECTED && !dependency.isAssignableFrom(context))
            throw new UnknownDependencyException(
                "Class " + context.getName() + " does not have permission to access protected service " +
                dependency.getName()
            );

        // check if the accessing context does not match the context the dependency was instantiated for
        else if (
            visibility == ServiceVisibility.CONTEXT &&
            !dependency.getClassLoader().equals(context.getClassLoader())
        ) {
            throw new UnknownDependencyException(
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
     * Manually update the value of the specified dependency type in the container cache.
     *
     * @param type the class type of the dependency
     * @param dependency the new instance of the dependency
     * @param <T> the type of the dependency
     */
    @Override
    @SneakyThrows
    public <T> void set(@NotNull Class<T> type, @NotNull T dependency) {
        set(type, dependency, Security.getCallerClass(Thread.currentThread().getStackTrace()));
    }

    /**
     * Manually update the value of the specified dependency type in the container cache.
     *
     * @param type the class type of the dependency
     * @param dependency the new instance of the dependency
     * @param context the class that the dependency was instantiated for
     */
    @Override
    public <T> void set(@NotNull Class<T> type, @NotNull T dependency, Class<?> context) {
        // validate that the service type annotates the service descriptor annotation
        Service service = type.getAnnotation(Service.class);
        if (service == null)
            throw new UnknownDependencyException("Class " + type.getName() + " is not a service");

        // use the root container to cache the singleton service
        ServiceScope scope = service.scope();
        if (scope == ServiceScope.SINGLETON && rootContainer != null)
            rootContainer.set(type, dependency, context);

        // do not set the instance of a transient service, as it is created every time it is accessed
        else if (scope == ServiceScope.TRANSIENT)
            throw new IllegalStateException("Cannot set transient service " + type.getName() + " in the container");

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
    @SneakyThrows
    private <TService> void handleTerminate(@NotNull TService value, @NotNull List<Method> methods) {
        for (Method method : methods)
            method.invoke(value);
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
     * Retrieve the list of all the registered dependencies in the container tree.
     *
     * @return each instance of the registered dependencies in the container tree
     */
    public @NotNull List<CachedDependency<?>> getTotalDependencies() {
        // make sure to start resolving the tree from the root container
        if (rootContainer instanceof DefaultContainerImpl)
            return ((DefaultContainerImpl) rootContainer).getTotalDependencies();

        // recursively resolve the dependencies from the child containers
        return new ArrayList<>(getLocalDependencies());
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
}
