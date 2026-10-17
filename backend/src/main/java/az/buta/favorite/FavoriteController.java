package az.buta.favorite;

import az.buta.catalog.ViewerResolver;
import az.buta.catalog.dto.OfferCardDto;
import az.buta.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/me/favorites")
public class FavoriteController {

    private final FavoriteService favorites;
    private final ViewerResolver viewers;

    public FavoriteController(FavoriteService favorites, ViewerResolver viewers) {
        this.favorites = favorites;
        this.viewers = viewers;
    }

    @GetMapping
    public List<OfferCardDto> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return favorites.list(viewers.resolve(user));
    }

    @PutMapping("/{offerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void add(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID offerId) {
        favorites.add(user.userId(), offerId);
    }

    @DeleteMapping("/{offerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID offerId) {
        favorites.remove(user.userId(), offerId);
    }
}
