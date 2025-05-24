package com.qibergames.divine.provider;

import com.qibergames.divine.descriptor.generic.InjectionType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a constructor injection information.
 */
@RequiredArgsConstructor
@Accessors(fluent = true)
@Getter
public class ConstructorInjectionHandle implements InjectionHandle {
    /**
     * The kind of the injection.
     */
    private final InjectionType type = InjectionType.CONSTRUCTOR_PARAMETER;

    /**
     * The service class that requested the dependency.
     */
    private final @NotNull Class<?> target;
}
