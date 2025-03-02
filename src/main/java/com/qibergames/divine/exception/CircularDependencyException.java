package com.qibergames.divine.exception;

import org.jetbrains.annotations.NotNull;

/**
 * Represents an exception that is thrown when two or more dependencies directly reference each other.
 */
public class CircularDependencyException extends GenericServiceException {
    /**
     * Initialize a new instance of the {@link CircularDependencyException} class.
     *
     * @param message the message that describes the error
     */
    public CircularDependencyException(@NotNull String message) {
        super(message);
    }
}
