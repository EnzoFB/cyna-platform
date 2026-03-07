package com.cyna.shared.domain;

/**
 * Base class for value objects.
 * Value objects are immutable and defined entirely by their attributes.
 * Subclasses must override equals(), hashCode(), and toString().
 *
 * Prefer using Java records which provide these automatically.
 */
public abstract class ValueObject {

    @Override
    public abstract boolean equals(Object o);

    @Override
    public abstract int hashCode();

    @Override
    public abstract String toString();
}
