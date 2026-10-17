package az.buta.favorite;

import az.buta.catalog.OfferCatalog;
import az.buta.catalog.OfferSnapshot;
import az.buta.catalog.OfferViewAssembler;
import az.buta.catalog.Viewer;
import az.buta.catalog.domain.OfferRepository;
import az.buta.catalog.dto.OfferCardDto;
import az.buta.common.error.ApiException;
import az.buta.recommendation.InteractionService;
import az.buta.recommendation.domain.InteractionType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class FavoriteService {

    private final FavoriteRepository favorites;
    private final OfferRepository offers;
    private final OfferCatalog catalog;
    private final OfferViewAssembler assembler;
    private final InteractionService interactions;
    private final Clock clock;

    public FavoriteService(FavoriteRepository favorites, OfferRepository offers, OfferCatalog catalog, OfferViewAssembler assembler,
                           InteractionService interactions, Clock clock) {
        this.favorites = favorites;
        this.offers = offers;
        this.catalog = catalog;
        this.assembler = assembler;
        this.interactions = interactions;
        this.clock = clock;
    }

    @Transactional
    public void add(UUID userId, UUID offerId) {
        var offer = offers.findById(offerId).orElseThrow(ApiException::notFound);
        Favorite.Key key = new Favorite.Key(userId, offerId);
        if (!favorites.existsById(key)) {
            favorites.save(new Favorite(userId, offerId, clock.instant()));
            interactions.record(userId, InteractionType.FAVORITE_ADD, offerId, offer.primaryCategory().getId(), null, null, null);
        }
    }

    @Transactional
    public void remove(UUID userId, UUID offerId) {
        Favorite.Key key = new Favorite.Key(userId, offerId);
        if (favorites.existsById(key)) {
            favorites.deleteById(key);
            interactions.record(userId, InteractionType.FAVORITE_REMOVE, offerId, null, null, null, null);
        }
    }

    @Transactional(readOnly = true)
    public List<OfferCardDto> list(Viewer viewer) {
        List<UUID> ids = favorites.findByUser(viewer.userId()).stream().map(Favorite::offerId).toList();
        Map<UUID, OfferSnapshot> snapshots = catalog.byIds(ids);
        return assembler.cards(ids.stream().map(snapshots::get).filter(Objects::nonNull).toList(), viewer);
    }
}
