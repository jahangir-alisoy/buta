package az.buta.membership;

import az.buta.common.domain.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "membership_purchases")
public class MembershipPurchase extends AbstractEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_code", nullable = false)
    private PlanCode planCode;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Column(name = "period_starts_at", nullable = false)
    private Instant periodStartsAt;

    @Column(name = "period_ends_at", nullable = false)
    private Instant periodEndsAt;

    @Column(name = "payment_provider", nullable = false)
    private String paymentProvider;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected MembershipPurchase() {
    }

    public static MembershipPurchase record(UUID userId, PlanCode planCode, BigDecimal amount, String currency, Instant periodStartsAt,
                                            Instant periodEndsAt, String paymentProvider, Instant now) {
        MembershipPurchase purchase = new MembershipPurchase();
        purchase.userId = userId;
        purchase.planCode = planCode;
        purchase.amount = amount;
        purchase.currency = currency;
        purchase.periodStartsAt = periodStartsAt;
        purchase.periodEndsAt = periodEndsAt;
        purchase.paymentProvider = paymentProvider;
        purchase.createdAt = now;
        return purchase;
    }
}
