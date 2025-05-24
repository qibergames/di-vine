package com.qibergames.divine.method;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

/**
 * Represents a service method information holder.
 */
public interface ServiceMethod {
    /**
     * The service that declares the method.
     *
     * @return the class type of the service
     */
    @NotNull Class<?> target();

    /**
     * The instance of the service.
     *
     * @return the instance of the class type
     */
    @NotNull Object instance();

    /**
     * The handle of the service method.
     *
     * @return the reflection access to the service method
     */
    @NotNull Method handle();

    /**
     * Invoke the inspected method with the specified arguments.
     * <p>
     * The service instance will be the target of the invocation.
     *
     * @param args the arguments to invoke the method with
     * @return the return value of the invocation
     */
    @Nullable Object invoke(@NotNull Object @Nullable ... args);
}
