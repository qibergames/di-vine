package com.atlas.divine.tree.cache;

import com.atlas.divine.descriptor.generic.Service;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

/**
 * Represents a data holder for a dependency instance that is being cached in a container.
 */
@RequiredArgsConstructor
@Accessors(fluent = true)
@Getter
public class Dependency {
    /**
     * The dependency instance that is being cached
     */
    private final Object value;

    /**
     * The descriptor of the dependency service
     */
    private final Service descriptor;

    /**
     * The context that the dependency was instantiated for.
     */
    private final Class<?> context;
}
