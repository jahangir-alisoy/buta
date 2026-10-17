package az.buta.membership.web;

import az.buta.membership.PlusPlanDto;
import az.buta.membership.PlusSubscriptionService;
import az.buta.security.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/plus")
public class PlusController {

    private final PlusSubscriptionService subscriptions;

    public PlusController(PlusSubscriptionService subscriptions) {
        this.subscriptions = subscriptions;
    }

    @GetMapping
    public PlusPlanDto plan(@AuthenticationPrincipal AuthenticatedUser user) {
        return subscriptions.plan(user.userId());
    }

    @PostMapping("/purchase")
    public PlusPlanDto purchase(@AuthenticationPrincipal AuthenticatedUser user) {
        return subscriptions.purchase(user);
    }
}
