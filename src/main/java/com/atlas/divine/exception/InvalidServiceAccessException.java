package com.atlas.divine.exception;

import org.jetbrains.annotations.NotNull;

public class InvalidServiceAccessException extends GenericServiceException {
    public InvalidServiceAccessException(@NotNull String message) {
        super(message);
    }
}
