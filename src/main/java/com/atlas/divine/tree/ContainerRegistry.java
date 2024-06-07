package com.atlas.divine.tree;

import com.atlas.divine.tree.cache.Dependency;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

/**
 * Represents a registry of container instances. Container registries may have child registries.
 * The main purpose of a container registry is to provide a way to organize and manage container instances.
 * <p>
 * You can retrieve a container registry by its specific name using the {@link #of(String)} method.
 */
public interface ContainerRegistry extends ContainerInstance {
    /**
     * Retrieve a container registry by its specific name.
     * If the parent registry already contains a registry with the specified name, it will be returned.
     * Otherwise, a new registry will be created and registered in the parent registry.
     *
     * @param name the name of the registry
     * @return the container registry with the specified name
     */
    @NotNull ContainerRegistry of(@NotNull String name);

    /**
     * Retrieve the root container registry of the container hierarchy.
     *
     * @return the global container registry
     */
    @NotNull ContainerRegistry ofGlobal();

    /**
     * Retrieve the list of dependencies that are registered in the container hierarchy.
     *
     * @return the list of all dependencies in the container hierarchy
     */
    @NotNull List<@NotNull Dependency> getDependencyTree();

    /**
     * Retrieve the set of container instances that are registered in the container hierarchy.
     *
     * @return the set of all container instances in the container hierarchy
     */
    @NotNull Set<@NotNull ContainerRegistry> getContainerTree();

    /**
     * Retrieve the name of this container registry implementation.
     *
     * @return the name of this container registry implementation
     */
    @NotNull String getName();

    /**
     * Retrieve the json representation of this container registry.
     *
     * @return the container data exported as json
     */
    @NotNull JsonObject export();
}
