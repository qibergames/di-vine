package com.qibergames.divine.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * Represents an enumeration of injection target types.
 */
@RequiredArgsConstructor
@Getter
public enum InjectionTarget {
    /**
     * `CLASS_FIELD` indicates, that the dependency should be injected into a class field.
     */
    CLASS_FIELD("class field"),

    /**
     * `CONSTRUCTOR_PARAMETER` indicates, that the dependency should be injected into a constructor parameter.
     */
    CONSTRUCTOR_PARAMETER("constructor parameter");

    /**
     * The display name of the injection target.
     */
    private final @NotNull String name;
}
