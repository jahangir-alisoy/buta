package az.buta.order.dto;

import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record RefundRequest(@Size(max = 20) List<UUID> orderItemIds, @Size(max = 200) String reason) {
}
