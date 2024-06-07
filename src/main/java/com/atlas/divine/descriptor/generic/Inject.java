package com.atlas.divine.descriptor.generic;

import com.atlas.divine.Container;
import com.atlas.divine.exception.UnknownDependencyException;
import com.atlas.divine.descriptor.property.PropertyProvider;
import com.atlas.divine.descriptor.factory.Factory;
import com.atlas.divine.descriptor.implementation.NoImplementation;
import com.atlas.divine.descriptor.property.NoPropertiesProvider;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Represents an annotation that indicates, that a certain field of a class
 * should be injected with a service. The framework will try to resolve the target service from the container
 * and will inject the value to the field.
 * <p>
 * Example:
 * <pre>
 *     &#64;Inject
 *     private MyService service;
 * </pre>
 *
 * @see Container
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Inject {
    /**
     * The placeholder string that indicates, that the injection descriptor does not intend to provide
     * string properties to the factory of the service.
     */
    String NO_PROPERTIES = "<NO PROPERTIES>";

    /**
     * The placeholder string that indicates, that the injection descriptor wants to resolve a dependency
     * from the container by its unique token.
     */
    String NO_TOKEN = "<NO TOKEN>";

    /**
     * The unique token value that is used to resolve the dependency from the container.
     * <p>
     * <b>Warning!</b> You cannot use this feature, if a property is specified.
     *
     * @return dependency unique token
     */
    @NotNull String token() default NO_TOKEN;

    /**
     * The string representation of properties that are passed to the service's factory, on the initialization
     * of the service.
     * <p>
     * Note that, this feature will only work, if the {@link Factory} requires {@link String} type of properties to be
     * passed to it. Otherwise, an {@link UnknownDependencyException} will be thrown.
     * <p>
     * <b>Warning!</b> You cannot use this feature, if a token is specified.
     *
     * @return the string of properties to be passed to the factory of the service
     */
    @NotNull String properties() default NO_PROPERTIES;

    /**
     * The provider class, that is used to pass properties of a specific kind to the factory of a service,
     * when it is injected into a field.
     * <p>
     * Note that, this feature will only work, if the type of the {@link PropertyProvider} matches the type of the
     * service {@link Factory}.
     * Otherwise, an {@link UnknownDependencyException} will be thrown.
     * <p>
     * <b>Warning!</b> You cannot use this feature, if a token is specified.
     *
     * @return the provider type, that is used to pass properties to the factory of a service
     */
    @NotNull Class<? extends PropertyProvider<?, ?>> provider() default NoPropertiesProvider.class;

    /**
     * Retrieve the class that will be used as the implementation of the service.
     * <p>
     * This is useful when the service is an interface or abstract class and there is no need for a factory.
     * <p>
     * By default, the annotated class will be used as the implementation.
     *
     * @return the service implementation class
     */
    @NotNull @ServiceLike Class<?> implementation() default NoImplementation.class;
}
