package com.qibergames.divine.exception;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a generic exception that is thrown when a service fails to initialize.
 * <p>
 * This exception is thrown when an error occurs caused on the user's end.
 */
public class ServiceInitializationException extends GenericServiceException {
    /**
     * Initialize a new instance of the {@link ServiceInitializationException} class.
     *
     * @param message the message that describes the error
     * @param cause the cause of the exception
     */
    public ServiceInitializationException(@NotNull String message, @NotNull Throwable cause) {
        super(message, cause);
    }

    /**
     * Initialize a new instance of the {@link ServiceInitializationException} class.
     *
     * @param message the message that describes the error
     */
    public ServiceInitializationException(@NotNull String message) {
        super(message);
    }
}
