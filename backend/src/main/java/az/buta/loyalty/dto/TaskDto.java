package az.buta.loyalty.dto;

import java.time.Instant;
import java.util.UUID;

public record TaskDto(UUID id, String code, String type, String title, String description, String terms, int progress, int target,
                      long rewardPoints, String period, String periodKey, String status, boolean awarded, Instant completedAt,
                      Instant endsAt) {
}
