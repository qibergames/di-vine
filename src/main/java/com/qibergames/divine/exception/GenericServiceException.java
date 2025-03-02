package com.qibergames.divine.exception;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a generic exception that is the base class of any exception that is thrown by the container.
 * <p>
 * Whenever accessing the container, it is guaranteed that it will only throw a subclass of this exception.
 */
public abstract class GenericServiceException extends RuntimeException {
    /**
     * Initialize a new instance of the {@link GenericServiceException} class.
     *
     * @param message the message that describes the error
     */
    public GenericServiceException(@NotNull String message, @NotNull Throwable cause) {
        super(message, cause);
    }

    /**
     * Initialize a new instance of the {@link GenericServiceException} class.
     *
     * @param message the message that describes the error
     */
    public GenericServiceException(@NotNull String message) {
        super(message);
    }
}
