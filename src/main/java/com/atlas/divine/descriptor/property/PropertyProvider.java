package com.atlas.divine.descriptor.property;

import com.atlas.divine.descriptor.generic.Service;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a provider functional interface, that is used to pass properties to the factory of a service,
 * when it is injected into a field.
 *
 * @param <TService> the type of the service to provide properties for
 * @param <TProperties> the type of the properties to provide
 */
public interface PropertyProvider<TService, TProperties> {
    /**
     * Provide the properties to the factory of a service, when it is injected into a field.
     *
     * @param descriptor the service descriptor of the dependency
     * @param type the type of the dependency that is passed to the dependency container
     * @param context the caller class that the container is being called from
     * @return the properties to be passed to the factory of a service
     */
    @NotNull TProperties provide(@NotNull Service descriptor, @NotNull Class<TService> type, @NotNull Class<?> context);
}
