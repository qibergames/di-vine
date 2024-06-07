package com.atlas.divine.descriptor.generic;

/**
 * Represents a registry of service instantiation rules.
 */
public enum ServiceScope {
    /**
     * `SINGLETON` indicates, that only one instance of the service should be created and shared globally.
     */
    SINGLETON,

    /**
     * `CONTAINER` indicates, that only one instance of the service should be created and shared within the container.
     * This means that different containers will have different instances of the same service.
     */
    CONTAINER,

    /**
     * `TRANSIENT` indicates, that a new instance of the service should be created every time it is requested.
     */
    TRANSIENT
}
