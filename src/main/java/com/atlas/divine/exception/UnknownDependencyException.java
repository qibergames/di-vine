package com.atlas.divine.exception;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a generic exception that is thrown when a dependency is requested, but is not found.
 */
public class UnknownDependencyException extends GenericServiceException {
    /**
     * Initialize a new instance of the {@link UnknownDependencyException} class.
     *
     * @param message the message that describes the error
     */
    public UnknownDependencyException(@NotNull String message) {
        super(message);
    }
}
