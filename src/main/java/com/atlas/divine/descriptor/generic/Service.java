package com.atlas.divine.descriptor.generic;

import com.atlas.divine.Container;
import com.atlas.divine.descriptor.factory.Factory;
import com.atlas.divine.descriptor.factory.NoFactory;
import com.atlas.divine.descriptor.implementation.NoImplementation;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Represents an annotation that indicates, that the annotated class should be treated as a service.
 * <p>
 * The framework will inject the service into the container and will manage its lifecycle.
 * Each field of the annotated class, that annotates {@link Inject} will be injected with the corresponding service.
 * <p>
 * You may use services for framework functions, where the required object annotates {@link ServiceLike}.
 *
 * @see Inject
 * @see ServiceLike
 * @see Container
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Service {
    /**
     * The default identifier of the service.
     */
    @NotNull String DEFAULT_ID = "<CLASS NAME>";

    /**
     * Retrieve the visibility rule of the service.
     *
     * @return the scope that the service can be accessed from
     */
    @NotNull ServiceVisibility visibility() default ServiceVisibility.GLOBAL;

    /**
     * Retrieve the instantiation rule of the service.
     *
     * @return the desired instantiation strategy for the service
     */
    @NotNull ServiceScope scope() default ServiceScope.SINGLETON;

    /**
     * Retrieve the unique identifier of the service.
     * <p>
     * By default, the signature of the class will be used as the identifier.
     *
     * @return the unique identifier of the service
     */
    @NotNull String id() default DEFAULT_ID;

    /**
     * Retrieve the factory that will create the instance for the service.
     *
     * @return the service instantiation factory
     */
    @NotNull Class<? extends Factory<?, ?>> factory() default NoFactory.class;

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

    /**
     * Retrieve the array of classes, that are allowed to implement this service interface.
     * <p>
     * If this service is not an interface or abstract class, you should not use this field.
     * <p>
     * This property is used, when the container explicitly implements the service interface, using the
     * {@link Container#implement(Class, Class)} method.
     * <p>
     * By default, any class can implement the service interface.
     * <p>
     * The {@link #implementation()} field is ignored from this rule, as it is specified explicitly.
     *
     * @return the array of classes that are allowed to implement this service interface
     */
    @NotNull @ServiceLike Class<?> @NotNull [] permits() default {};
}
