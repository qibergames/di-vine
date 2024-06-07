package com.atlas.divine.descriptor.implementation;

/**
 * Represents a placeholder class that indicates, that the service did not specify an implementation class.
 */
public class NoImplementation {
    /**
     * Prevent the initialization of the class.
     */
    private NoImplementation() {
        throw new IllegalStateException("Should not initialize non-specified implementation.");
    }
}
