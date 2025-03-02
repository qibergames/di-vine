package com.qibergames.divine.impl;

import com.qibergames.divine.descriptor.factory.NoFactory;
import com.qibergames.divine.descriptor.implementation.NoImplementation;
import com.qibergames.divine.runtime.lifecycle.BeforeTerminate;
import com.qibergames.divine.descriptor.generic.Service;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Represents a data holder for a dependency instance that is being cached in a container.
 *
 * @param <T> the type of the dependency instance
 */
@RequiredArgsConstructor
@Accessors(fluent = true)
@Getter
public class CachedDependency<T> {
    /**
     * The dependency instance that is being cached.
     */
    private final @NotNull T value;

    /**
     * The descriptor of the dependency service.
     */
    private final @NotNull Service descriptor;

    /**
     * The context that the dependency was instantiated for.
     */
    private final @NotNull Class<?> context;

    /**
     * The list of methods that are annotated with the {@link BeforeTerminate} annotation.
     */
    private final @NotNull List<Method> terminators;

    /**
     * Retrieve the json representation of this cached dependency.
     *
     * @return the cached dependency exported as json
     */
    public @NotNull JsonObject export() {
        JsonObject element = new JsonObject();
        element.addProperty("type", value.getClass().getName());
        element.addProperty("value", value.toString());

        element.add("descriptor", exportDescriptor());

        element.addProperty("context", context.getName());

        JsonArray terminators = new JsonArray();
        for (Method terminator : this.terminators)
            terminators.add(terminator.getName());

        element.add("terminators", terminators);

        return element;
    }

    /**
     * Retrieve the json representation of the descriptor of the cached dependency.
     *
     * @return the cached dependency descriptor exported as json
     */
    private @NotNull JsonObject exportDescriptor() {
        JsonObject service = new JsonObject();

        service.addProperty("visibility", descriptor.visibility().name());

        service.addProperty("scope", descriptor.scope().name());
        String id = !descriptor.id().equals(Service.DEFAULT_ID) ? descriptor.id() : null;
        service.addProperty("id", id);

        String factory = descriptor.factory() != NoFactory.class ? descriptor.factory().getName() : null;
        service.addProperty("factory", factory);

        String implementation = descriptor.implementation() != NoImplementation.class ?
            descriptor.implementation().getName() :
            null;
        service.addProperty("implementation", implementation);

        JsonArray permits = new JsonArray();
        for (Class<?> permit : descriptor.permits())
            permits.add(permit.getName());
        service.add("permits", !permits.isEmpty() ? permits : null);

        if (descriptor.multiple())
            service.addProperty("multiple", true);

        return service;
    }
}
