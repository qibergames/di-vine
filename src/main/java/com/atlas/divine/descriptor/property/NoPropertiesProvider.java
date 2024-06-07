package com.atlas.divine.descriptor.property;

import com.atlas.divine.descriptor.generic.Inject;
import com.atlas.divine.descriptor.generic.Service;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a dummy implementation for the property provider, that indicates, that the {@link Inject} annotation
 * does not specify a provider for any properties to be passed to the factory of a service,
 * when it is injected into a field.
 */
public class NoPropertiesProvider implements PropertyProvider<Object, Object> {
    /**
     * Provide the properties to the factory of a service, when it is injected into a field.
     *
     * @param descriptor the service descriptor of the dependency
     * @param type the type of the dependency that is passed to the dependency container
     * @param context the caller class that the container is being called from
     * @return the properties to be passed to the factory of a service
     */
    @Override
    @SuppressWarnings("rawtypes")
    public Object provide(@NotNull Service descriptor, @NotNull Class type, @NotNull Class context) {
        throw new IllegalStateException("This should never be called.");
    }
}
