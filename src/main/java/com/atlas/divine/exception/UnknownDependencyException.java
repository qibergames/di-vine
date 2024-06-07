package com.atlas.divine.exception;

import com.atlas.divine.Container;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a generic exception that is thrown when a dependency is not found or is invalid.
 * <p>
 * Whenever accessing the {@link Container}, it is guaranteed, that it would only throw this exception, if the fault
 * is caused by invalid logic during injection or missing dependencies.
 */
public class UnknownDependencyException extends RuntimeException {
    /**
     * Initialize a new instance of the {@link UnknownDependencyException} class.
     * @param message the message that describes the error.
     */
    public UnknownDependencyException(@NotNull String message) {
        super(message);
    }
}
