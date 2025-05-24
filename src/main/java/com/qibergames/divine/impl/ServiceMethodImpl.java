package com.qibergames.divine.impl;

import com.qibergames.divine.exception.ServiceRuntimeException;
import com.qibergames.divine.method.ServiceMethod;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

/**
 * Represents a service method information holder.
 */
@RequiredArgsConstructor
@Accessors(fluent = true)
@Getter
public class ServiceMethodImpl implements ServiceMethod {
    /**
     * The service that declares the method.
     */
    private final Class<?> target;

    /**
     * The instance of the service.
     */
    private final Object instance;

    /**
     * The handle of the service method.
     */
    private final Method handle;

    /**
     * Invoke the inspected method with the specified arguments.
     * <p>
     * The service instance will be the target of the invocation.
     *
     * @param args the arguments to invoke the method with
     * @return the return value of the invocation
     */
    @Override
    public @Nullable Object invoke(@NotNull Object @Nullable ... args) {
        try {
            handle.setAccessible(true);
            return handle.invoke(instance, args);
        } catch (Throwable t) {
            throw new ServiceRuntimeException("Unable to invoke service method `" + handle.getName() + "`", t);
        }
    }
}
