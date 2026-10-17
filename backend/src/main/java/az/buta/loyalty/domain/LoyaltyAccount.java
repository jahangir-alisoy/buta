package az.buta.loyalty.domain;

import az.buta.common.domain.NewAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "loyalty_accounts")
public class LoyaltyAccount extends NewAwareEntity<UUID> {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "available_points", nullable = false)
    private long availablePoints;

    @Column(name = "lifetime_earned_points", nullable = false)
    private long lifetimeEarnedPoints;

    @Column(name = "lifetime_redeemed_points", nullable = false)
    private long lifetimeRedeemedPoints;

    @Column(name = "tier_qualifying_points", nullable = false)
    private long tierQualifyingPoints;

    @Column(name = "tier_code", nullable = false)
    private String tierCode;

    @Column(name = "flagged_for_review", nullable = false)
    private boolean flaggedForReview;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected LoyaltyAccount() {
    }

    public static LoyaltyAccount open(UUID userId, String tierCode, Instant now) {
        LoyaltyAccount account = new LoyaltyAccount();
        account.userId = userId;
        account.tierCode = tierCode;
        account.updatedAt = now;
        return account;
    }

    @Override
    public UUID getId() {
        return userId;
    }

    public void apply(LedgerEntryType type, long points, LedgerEntryType reversedType, Instant now) {
        availablePoints += points;
        switch (type) {
            case EARN_PURCHASE, EARN_TASK -> {
                lifetimeEarnedPoints += points;
                tierQualifyingPoints += points;
            }
            case REDEEM -> lifetimeRedeemedPoints += -points;
            case ADJUSTMENT -> {
                lifetimeEarnedPoints += points;
                tierQualifyingPoints += points;
            }
            case REVERSAL -> {
                if (reversedType == LedgerEntryType.REDEEM) {
                    lifetimeRedeemedPoints += -points;
                } else {
                    lifetimeEarnedPoints += points;
                    tierQualifyingPoints += points;
                }
            }
        }
        flaggedForReview = availablePoints < 0;
        updatedAt = now;
    }

    public void assignTier(String tierCode) {
        this.tierCode = tierCode;
    }

    public UUID userId() {
        return userId;
    }

    public long availablePoints() {
        return availablePoints;
    }

    public long lifetimeEarnedPoints() {
        return lifetimeEarnedPoints;
    }

    public long lifetimeRedeemedPoints() {
        return lifetimeRedeemedPoints;
    }

    public long tierQualifyingPoints() {
        return tierQualifyingPoints;
    }

    public String tierCode() {
        return tierCode;
    }

    public boolean flaggedForReview() {
        return flaggedForReview;
    }
}
