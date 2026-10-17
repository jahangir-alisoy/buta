package az.buta.recommendation.web;

import az.buta.catalog.ViewerResolver;
import az.buta.recommendation.InteractionService;
import az.buta.recommendation.InterestProfileService;
import az.buta.recommendation.Placement;
import az.buta.recommendation.RecommendationService;
import az.buta.recommendation.domain.InteractionType;
import az.buta.recommendation.dto.InteractionBatch;
import az.buta.recommendation.dto.RecommendationsDto;
import az.buta.common.error.RateLimitedException;
import az.buta.common.error.ErrorCode;
import az.buta.security.AuthenticatedUser;
import az.buta.security.RateLimitDecision;
import az.buta.security.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.UUID;

@RestController
@Validated
@RequestMapping("/api/v1")
public class RecommendationController {

    private final RecommendationService recommendations;
    private final InteractionService interactions;
    private final InterestProfileService profiles;
    private final ViewerResolver viewers;
    private final RateLimiter rateLimiter;

    public RecommendationController(RecommendationService recommendations, InteractionService interactions, InterestProfileService profiles,
                                    ViewerResolver viewers, RateLimiter rateLimiter) {
        this.recommendations = recommendations;
        this.interactions = interactions;
        this.profiles = profiles;
        this.viewers = viewers;
        this.rateLimiter = rateLimiter;
    }

    @GetMapping("/recommendations")
    public RecommendationsDto recommendations(@AuthenticationPrincipal AuthenticatedUser user,
                                              @RequestParam(defaultValue = "HOME") Placement placement,
                                              @RequestParam(required = false) UUID offerId,
                                              @RequestParam(defaultValue = "12") @Min(1) @Max(24) int limit) {
        return recommendations.recommend(viewers.resolve(user), placement, offerId, limit);
    }

    @PostMapping("/interactions")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void interactions(@AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody InteractionBatch batch,
                             HttpServletRequest request) {
        String key = user == null ? "interactions-ip:" + request.getRemoteAddr() : "interactions-user:" + user.userId();
        RateLimitDecision decision = rateLimiter.tryAcquire(key, 120, Duration.ofMinutes(1));
        if (!decision.allowed()) {
            throw new RateLimitedException(ErrorCode.RATE_LIMITED, decision.retryAfter());
        }
        UUID userId = user == null ? null : user.userId();
        batch.events().forEach(event -> interactions.record(userId, InteractionType.valueOf(event.type().name()), event.offerId(),
                event.categoryId(), event.query(), event.requestId(), event.placement()));
    }

    @DeleteMapping("/me/personalization-data")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reset(@AuthenticationPrincipal AuthenticatedUser user) {
        profiles.reset(user.userId());
    }
}
