package az.buta.catalog;

import az.buta.membership.MembershipFeature;
import az.buta.membership.MembershipStatus;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public record Viewer(UUID userId, MembershipStatus membership, BigDecimal tierMultiplier, Set<UUID> favoriteOfferIds, Locale locale) {

    public boolean isAuthenticated() {
        return userId != null;
    }

    public boolean has(MembershipFeature feature) {
        return membership != null && membership.has(feature);
    }

    public boolean isFavorite(UUID offerId) {
        return favoriteOfferIds.contains(offerId);
    }
}
