package com.qibergames.divine.descriptor.factory;

import com.qibergames.divine.descriptor.generic.Service;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a factory implementation that is a placeholder for not specifying a factory type.
 */
public class NoFactory implements Factory<Object, Object> {
    /**
     * Create a new instance of type {@code T}.
     *
     * @param descriptor the service descriptor of the dependency
     * @param type the type of the dependency that is passed to the dependency container
     * @param context the caller class that the container is being called from
     * @param properties the properties to create the instance with
     * @return a new instance of type {@code T}.
     */
    @Override
    @Contract("_, _, _, _ -> fail")
    public @NotNull Object create(
        @NotNull Service descriptor, @NotNull Class<?> type, @NotNull Class<?> context,
        @Nullable Object properties
    ) {
        throw new IllegalStateException("This should never be called.");
    }
}
