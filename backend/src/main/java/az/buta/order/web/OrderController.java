package az.buta.order.web;

import az.buta.common.web.PageResponse;
import az.buta.order.OrderLifecycleService;
import az.buta.order.OrderQueryService;
import az.buta.order.RefundService;
import az.buta.order.dto.OrderDto;
import az.buta.order.dto.OrderSummaryDto;
import az.buta.order.dto.RefundRequest;
import az.buta.security.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@Validated
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderQueryService queries;
    private final OrderLifecycleService lifecycle;
    private final RefundService refunds;

    public OrderController(OrderQueryService queries, OrderLifecycleService lifecycle, RefundService refunds) {
        this.queries = queries;
        this.lifecycle = lifecycle;
        this.refunds = refunds;
    }

    @GetMapping
    public PageResponse<OrderSummaryDto> list(@AuthenticationPrincipal AuthenticatedUser user,
                                              @RequestParam(defaultValue = "0") @Min(0) int page,
                                              @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return queries.list(user.userId(), page, size, LocaleContextHolder.getLocale());
    }

    @GetMapping("/{orderId}")
    public OrderDto get(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID orderId) {
        return queries.get(user.userId(), orderId, LocaleContextHolder.getLocale());
    }

    @PostMapping("/{orderId}/cancel")
    public OrderDto cancel(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID orderId) {
        return lifecycle.cancelByCustomer(user.userId(), orderId, LocaleContextHolder.getLocale());
    }

    @PostMapping("/{orderId}/refund")
    public OrderDto refund(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID orderId, @Valid @RequestBody RefundRequest request) {
        return refunds.refundByCustomer(user.userId(), orderId, request.orderItemIds(), request.reason(), LocaleContextHolder.getLocale());
    }
}
