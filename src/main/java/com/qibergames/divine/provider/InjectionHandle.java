package com.qibergames.divine.provider;

import com.qibergames.divine.descriptor.generic.InjectionType;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a field or constructor injection information.
 */
public interface InjectionHandle {
    /**
     * The service class that requested the dependency.
     *
     * @return the dependency owner class type
     */
    @NotNull Class<?> target();

    /**
     * The kind of the injection.
     *
     * @return either {@link InjectionType#CLASS_FIELD} or {@link InjectionType#CONSTRUCTOR_PARAMETER}
     */
    @NotNull InjectionType type();
}
