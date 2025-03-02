package com.qibergames.divine.exception;

import org.jetbrains.annotations.NotNull;

/**
 * Represents an exception that is thrown, when the service access is denied.
 */
public class InvalidServiceAccessException extends GenericServiceException {
    /**
     * Initialize a new instance of the {@link InvalidServiceAccessException} class.
     *
     * @param message the message that describes the error
     */
    public InvalidServiceAccessException(@NotNull String message) {
        super(message);
    }
}
