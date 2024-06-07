package com.atlas.divine.impl;

import com.atlas.divine.runtime.lifecycle.BeforeTerminate;
import com.atlas.divine.descriptor.generic.Service;
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
}
