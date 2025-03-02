package com.qibergames.divine.descriptor.generic;

/**
 * Represents a registry of service visibility rules.
 */
public enum ServiceVisibility {
    /**
     * `PRIVATE` indicates, that a service instance will be only accessible from the class that it was created from.
     */
    PRIVATE,

    /**
     * `PROTECTED` indicates, that a service instance will be only accessible from the class that it was created from
     * and any subclass of that class.
     */
    PROTECTED,

    /**
     * `CONTEXT` indicates, that a service instance will be only accessible from the context that the service was
     * created from.
     */
    CONTEXT,

    /**
     * `GLOBAL` indicates, that a service instance will be accessible from any context.
     */
    GLOBAL
}
