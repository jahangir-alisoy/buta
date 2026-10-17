package az.buta.order;

import az.buta.common.error.ApiException;
import az.buta.common.web.PageResponse;
import az.buta.order.domain.OrderRepository;
import az.buta.order.domain.OrderStatus;
import az.buta.order.dto.OrderDto;
import az.buta.order.dto.OrderSummaryDto;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
public class OrderQueryService {

    private final OrderRepository orders;
    private final OrderViewAssembler views;

    public OrderQueryService(OrderRepository orders, OrderViewAssembler views) {
        this.orders = orders;
        this.views = views;
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryDto> list(UUID userId, int page, int size, Locale locale) {
        return PageResponse.of(orders.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size)), order -> views.summary(order, locale));
    }

    @Transactional(readOnly = true)
    public OrderDto get(UUID userId, UUID orderId, Locale locale) {
        return views.toDto(orders.findByIdAndUserId(orderId, userId).orElseThrow(ApiException::notFound), locale);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryDto> search(OrderStatus status, String query, int page, int size, Locale locale) {
        String pattern = query == null || query.isBlank() ? null : "%" + query.strip().toUpperCase(Locale.ROOT) + "%";
        return PageResponse.of(orders.search(status, pattern, PageRequest.of(page, size)), order -> views.summary(order, locale));
    }

    @Transactional(readOnly = true)
    public OrderDto adminGet(UUID orderId, Locale locale) {
        return views.toDto(orders.findById(orderId).orElseThrow(ApiException::notFound), locale, true);
    }
}
