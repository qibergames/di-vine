package com.qibergames.divine.exception;

import org.jetbrains.annotations.NotNull;

/**
 * Represents an error that occurs, whilst executing a service runtime operation.
 */
public class ServiceRuntimeException extends GenericServiceException {
    /**
     * Initialize a new instance of the {@link ServiceRuntimeException} class.
     *
     * @param message the message that describes the error
     * @param cause the cause of the error
     */
    public ServiceRuntimeException(@NotNull String message, @NotNull Throwable cause) {
        super(message, cause);
    }

    /**
     * Initialize a new instance of the {@link ServiceRuntimeException} class.
     *
     * @param message the message that describes the error
     */
    public ServiceRuntimeException(@NotNull String message) {
        super(message);
    }
}
