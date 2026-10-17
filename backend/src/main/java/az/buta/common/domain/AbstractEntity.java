package az.buta.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import java.util.Objects;
import java.util.UUID;

@MappedSuperclass
public abstract class AbstractEntity extends NewAwareEntity<UUID> {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    protected AbstractEntity() {
        this.id = UUID.randomUUID();
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        return Objects.equals(id, ((AbstractEntity) other).id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
