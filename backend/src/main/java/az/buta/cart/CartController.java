package az.buta.cart;

import az.buta.catalog.ViewerResolver;
import az.buta.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cart")
public class CartController {

    private final CartService carts;
    private final ViewerResolver viewers;

    public CartController(CartService carts, ViewerResolver viewers) {
        this.carts = carts;
        this.viewers = viewers;
    }

    @GetMapping
    public CartDto view(@AuthenticationPrincipal AuthenticatedUser user) {
        return carts.view(viewers.resolve(user));
    }

    @PostMapping("/items")
    public CartDto add(@AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody AddCartItemRequest request) {
        return carts.add(viewers.resolve(user), request);
    }

    @PatchMapping("/items/{itemId}")
    public CartDto update(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID itemId,
                          @Valid @RequestBody UpdateCartItemRequest request) {
        return carts.updateQuantity(viewers.resolve(user), itemId, request.quantity());
    }

    @DeleteMapping("/items/{itemId}")
    public CartDto remove(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID itemId) {
        return carts.remove(viewers.resolve(user), itemId);
    }
}
