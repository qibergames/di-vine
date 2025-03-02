package com.qibergames.divine.descriptor.factory;

import com.qibergames.divine.descriptor.generic.Service;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a factory that creates instances of type {@code T}.
 *
 * @param <TService> the type of the instances to create.
 * @param <TProperties> the type of the properties to create the instances with.
 */
public interface Factory<TService, TProperties> {
    /**
     * Create a new instance of type {@code T}.
     * <p>
     * This method should <b>always</b> create a new instance of the type {@code T}, as the dependency
     * injector will implicitly cache the instances, as specified in the {@link Service} descriptor.
     *
     * @param descriptor the service descriptor of the dependency
     * @param type the type of the dependency that is passed to the dependency container
     * @param context the caller class that the container is being called from
     * @param properties the properties to create the instance with
     * @return a new instance of type {@code T}.
     */
    @Contract("_, _, _, _ -> new")
    @NotNull TService create(
        @NotNull Service descriptor, @NotNull Class<? extends TService> type, @NotNull Class<?> context,
        @Nullable TProperties properties
    );
}
