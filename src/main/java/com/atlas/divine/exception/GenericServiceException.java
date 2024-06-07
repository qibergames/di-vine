package com.atlas.divine.exception;

public abstract class GenericServiceException extends RuntimeException {
    public GenericServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public GenericServiceException(String message) {
        super(message);
    }
}
