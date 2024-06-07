package com.atlas.divine.exception;

import org.jetbrains.annotations.NotNull;

/**
 * Represents an exception that occurs when registering or accessing a service that is invalid.
 */
public class InvalidServiceException extends GenericServiceException {
    /**
     * Initialize a new instance of the {@link InvalidServiceException} class.
     *
     * @param message the message that describes the error.
     */
    public InvalidServiceException(@NotNull String message) {
        super(message);
    }
}
