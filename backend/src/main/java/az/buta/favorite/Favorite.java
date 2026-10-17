package az.buta.favorite;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.springframework.data.domain.Persistable;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "favorites")
public class Favorite implements Persistable<Favorite.Key> {

    @EmbeddedId
    private Key key;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @jakarta.persistence.Transient
    private boolean fresh = true;

    protected Favorite() {
    }

    public Favorite(UUID userId, UUID offerId, Instant createdAt) {
        this.key = new Key(userId, offerId);
        this.createdAt = createdAt;
    }

    @Override
    public Key getId() {
        return key;
    }

    @Override
    public boolean isNew() {
        return fresh;
    }

    @jakarta.persistence.PostLoad
    @jakarta.persistence.PostPersist
    void markPersisted() {
        fresh = false;
    }

    public UUID offerId() {
        return key.offerId;
    }

    public Instant createdAt() {
        return createdAt;
    }

    @Embeddable
    public record Key(@Column(name = "user_id") UUID userId, @Column(name = "offer_id") UUID offerId) implements Serializable {
    }
}
