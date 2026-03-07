package com.cyna.shared.domain;

import java.util.Objects;

/**
 * Base class for entities.
 * An entity is identified by its unique ID and has a lifecycle.
 *
 * @param <ID> the type of the entity's identifier
 */
public abstract class Entity<ID> {

    private final ID id;

    protected Entity(ID id) {
        if (id == null) throw new IllegalArgumentException("Entity ID must not be null");
        this.id = id;
    }

    public ID getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Entity<?> entity = (Entity<?>) o;
        return Objects.equals(id, entity.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
