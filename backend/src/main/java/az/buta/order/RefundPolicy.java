package az.buta.order;

import az.buta.catalog.domain.PartnerProduct;
import az.buta.order.domain.Order;
import az.buta.order.domain.OrderItem;
import az.buta.order.domain.OrderStatus;
import az.buta.order.domain.Redemption;
import az.buta.order.domain.RedemptionStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
public class RefundPolicy {

    public String blockReason(Order order, OrderItem item, Map<UUID, PartnerProduct> products, Instant now, boolean admin) {
        if (order.status() != OrderStatus.FULFILLED && order.status() != OrderStatus.PARTIALLY_REFUNDED) {
            return "ORDER_STATE";
        }
        if (item.isRefunded()) {
            return "ALREADY_REFUNDED";
        }
        if (item.redemptions().stream().map(Redemption::status).anyMatch(status -> status != RedemptionStatus.ISSUED)) {
            return "ALREADY_USED";
        }
        if (admin) {
            return null;
        }
        for (Redemption redemption : item.redemptions()) {
            PartnerProduct product = products.get(redemption.productId());
            if (product == null || !product.refundable()) {
                return "NOT_REFUNDABLE";
            }
            if (now.isAfter(product.refundDeadline())) {
                return "DEADLINE_PASSED";
            }
        }
        return null;
    }
}
