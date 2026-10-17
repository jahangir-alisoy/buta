package az.buta.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.Instant;
import java.util.Objects;

@Embeddable
public class RoleGrant {

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false)
    private RoleSource source;

    @Column(name = "granted_at", nullable = false)
    private Instant grantedAt;

    protected RoleGrant() {
    }

    public RoleGrant(Role role, RoleSource source, Instant grantedAt) {
        this.role = role;
        this.source = source;
        this.grantedAt = grantedAt;
    }

    public Role role() {
        return role;
    }

    public RoleSource source() {
        return source;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof RoleGrant grant && grant.role == role;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(role);
    }
}
