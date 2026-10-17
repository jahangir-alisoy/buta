package az.buta.membership;

import az.buta.common.domain.NewAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "memberships")
public class Membership extends NewAwareEntity<UUID> {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_code", nullable = false)
    private PlanCode planCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MembershipSource source;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at")
    private Instant endsAt;

    @Column(name = "granted_by")
    private UUID grantedBy;

    private String reason;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected Membership() {
    }

    public static Membership free(UUID userId, Instant now) {
        Membership membership = new Membership();
        membership.userId = userId;
        membership.planCode = PlanCode.FREE;
        membership.source = MembershipSource.DEFAULT;
        membership.startsAt = now;
        membership.updatedAt = now;
        return membership;
    }

    @Override
    public UUID getId() {
        return userId;
    }

    public boolean isPlusAt(Instant now) {
        return planCode == PlanCode.PLUS && (endsAt == null || now.isBefore(endsAt));
    }

    public void grantPlus(MembershipSource newSource, Instant endsAt, UUID grantedBy, String reason, Instant now) {
        if (planCode != PlanCode.PLUS || !isPlusAt(now)) {
            this.startsAt = now;
        }
        this.planCode = PlanCode.PLUS;
        this.source = newSource;
        this.endsAt = endsAt;
        this.grantedBy = grantedBy;
        this.reason = reason;
        this.updatedAt = now;
    }

    public void downgrade(String reason, UUID actor, Instant now) {
        this.planCode = PlanCode.FREE;
        this.source = MembershipSource.DEFAULT;
        this.startsAt = now;
        this.endsAt = null;
        this.grantedBy = actor;
        this.reason = reason;
        this.updatedAt = now;
    }

    public UUID userId() {
        return userId;
    }

    public PlanCode planCode() {
        return planCode;
    }

    public MembershipSource source() {
        return source;
    }

    public Instant startsAt() {
        return startsAt;
    }

    public Instant endsAt() {
        return endsAt;
    }
}
