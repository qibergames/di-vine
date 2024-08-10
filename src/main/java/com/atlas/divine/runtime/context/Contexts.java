package com.atlas.divine.runtime.context;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a utility class that resolves call context classes.
 * <p>
 * The call contexts are used to separate services based on scopes.
 */
@UtilityClass
public class Contexts {
    /**
     * Get the caller class of the method that called {@link Thread#getStackTrace()}.
     *
     * @param stackTrace the stack trace of the method that's caller should be resolved
     * @return the caller class of the method that resolved the stack trace
     *
     * @throws ClassNotFoundException if the caller class cannot be found
     */
    public @NotNull Class<?> getCallerClass(
        @NotNull StackTraceElement @NotNull [] stackTrace
    ) throws ClassNotFoundException {
        if (stackTrace.length > 2) {
            String callerClassName = stackTrace[2].getClassName();
            return Class.forName(callerClassName);
        }

        throw new ClassNotFoundException("Unable to determine caller class");
    }

    /**
     * Get the caller class of the method that this method is called from.
     *
     * @return the caller class of the method that this method is called from
     *
     * @throws ClassNotFoundException if the caller class cannot be found
     */
    public @NotNull Class<?> getCallerClass() throws ClassNotFoundException {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        if (stackTrace.length > 3) {
            String callerClassName = stackTrace[3].getClassName();
            return Class.forName(callerClassName);
        }

        throw new ClassNotFoundException("Unable to determine caller class");
    }
}
