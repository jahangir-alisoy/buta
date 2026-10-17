package az.buta.order.dto;

import az.buta.common.money.MoneyDto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderDto(UUID id, String orderNumber, String status, MoneyDto subtotal, long pointsRedeemed, MoneyDto pointsDiscount,
                       MoneyDto total, long pointsEarned, Instant createdAt, Instant expiresAt, Instant fulfilledAt, Instant completedAt,
                       Instant cancelledAt, String cancelReason, PaymentDto payment, List<Item> items, List<RefundDto> refunds,
                       boolean cancellable, boolean awaitingPayment) {

    public record PaymentDto(UUID id, String provider, String mode, String status, MoneyDto amount, MoneyDto refunded) {
    }

    public record Item(UUID id, UUID offerId, String title, String offerType, int quantity, MoneyDto unitPrice, MoneyDto cashPaid,
                       long pointsEarned, boolean refunded, boolean refundable, String refundBlockReason, List<Voucher> vouchers) {
    }

    public record Voucher(String code, String status, String productTitle, String partnerName, Instant validFrom, Instant validUntil,
                          Instant eventStartsAt, String venueName, String redemptionMethod, String redemptionInstructions) {
    }

    public record RefundDto(MoneyDto amount, String initiatedBy, String reason, Instant createdAt) {
    }
}
