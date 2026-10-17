package az.buta.loyalty.web;

import az.buta.common.web.PageResponse;
import az.buta.loyalty.LoyaltyQueryService;
import az.buta.loyalty.dto.LedgerEntryDto;
import az.buta.loyalty.dto.LoyaltySummaryDto;
import az.buta.loyalty.dto.ReferralInfoDto;
import az.buta.loyalty.dto.TaskDto;
import az.buta.security.AuthenticatedUser;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api/v1/me")
public class LoyaltyController {

    private final LoyaltyQueryService loyalty;

    public LoyaltyController(LoyaltyQueryService loyalty) {
        this.loyalty = loyalty;
    }

    @GetMapping({"/loyalty", "/membership"})
    public LoyaltySummaryDto summary(@AuthenticationPrincipal AuthenticatedUser user) {
        return loyalty.summary(user.userId(), LocaleContextHolder.getLocale());
    }

    @GetMapping("/loyalty/ledger")
    public PageResponse<LedgerEntryDto> ledger(@AuthenticationPrincipal AuthenticatedUser user,
                                               @RequestParam(defaultValue = "0") @Min(0) int page,
                                               @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return loyalty.ledger(user.userId(), page, size);
    }

    @GetMapping("/loyalty/tasks")
    public List<TaskDto> tasks(@AuthenticationPrincipal AuthenticatedUser user) {
        return loyalty.tasks(user.userId(), LocaleContextHolder.getLocale());
    }

    @GetMapping("/referral")
    public ReferralInfoDto referral(@AuthenticationPrincipal AuthenticatedUser user) {
        return loyalty.referral(user.userId());
    }
}
