package az.buta.catalog;

import az.buta.favorite.Favorite;
import az.buta.favorite.FavoriteRepository;
import az.buta.loyalty.domain.LoyaltyAccountRepository;
import az.buta.loyalty.domain.LoyaltyTier;
import az.buta.loyalty.domain.LoyaltyTierRepository;
import az.buta.membership.MembershipService;
import az.buta.security.AuthenticatedUser;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ViewerResolver {

    private final MembershipService membership;
    private final FavoriteRepository favorites;
    private final LoyaltyAccountRepository accounts;
    private final LoyaltyTierRepository tiers;

    public ViewerResolver(MembershipService membership, FavoriteRepository favorites, LoyaltyAccountRepository accounts,
                          LoyaltyTierRepository tiers) {
        this.membership = membership;
        this.favorites = favorites;
        this.accounts = accounts;
        this.tiers = tiers;
    }

    @Transactional(readOnly = true)
    public Viewer resolve(AuthenticatedUser principal) {
        if (principal == null) {
            return new Viewer(null, null, BigDecimal.ONE, Set.of(), LocaleContextHolder.getLocale());
        }
        BigDecimal multiplier = accounts.findById(principal.userId())
                .flatMap(account -> tiers.findById(account.tierCode()))
                .map(LoyaltyTier::earnMultiplier)
                .orElse(BigDecimal.ONE);
        Set<java.util.UUID> favoriteIds = favorites.findByUser(principal.userId()).stream()
                .map(Favorite::offerId)
                .collect(Collectors.toSet());
        return new Viewer(principal.userId(), membership.current(principal.userId()), multiplier, favoriteIds, LocaleContextHolder.getLocale());
    }
}
