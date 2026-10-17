package az.buta.loyalty.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record LedgerEntryDto(UUID id, String type, long points, String descriptionKey, Map<String, String> params, Instant createdAt) {
}
