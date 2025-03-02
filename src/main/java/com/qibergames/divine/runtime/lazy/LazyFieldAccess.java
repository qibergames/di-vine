package com.qibergames.divine.runtime.lazy;

import com.qibergames.divine.descriptor.generic.Inject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a class field that should be lazily injected by the container.
 */
@RequiredArgsConstructor
@Getter
public class LazyFieldAccess {
    /**
     * The instance of the class that contains the field.
     */
    private final @NotNull Object instance;

    /**
     * The type of the requesting service.
     */
    private final @NotNull Class<?> type;

    /**
     * The injection descriptor of the field.
     */
    private final @NotNull Inject descriptor;

    /**
     * The caller class that requested the injection.
     */
    private final @NotNull Class<?> context;
}
