package com.qibergames.divine.provider;

import com.qibergames.divine.descriptor.generic.InjectionType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;

/**
 * Represents a field injection information.
 */
@RequiredArgsConstructor
@Accessors(fluent = true)
@Getter
public class FieldInjectionHandle implements InjectionHandle {
    /**
     * The kind of the injection.
     */
    public final InjectionType type = InjectionType.CLASS_FIELD;

    /**
     * The service class that requested the dependency.
     */
    private final @NotNull Class<?> target;

    /**
     * The instance of the requesting class.
     */
    private final @NotNull Object instance;

    /**
     * The field that the container want to inject into.
     */
    private final @NotNull Field field;
}
