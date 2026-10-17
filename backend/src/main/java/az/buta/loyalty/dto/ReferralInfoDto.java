package az.buta.loyalty.dto;

import java.time.Instant;
import java.util.List;

public record ReferralInfoDto(String code, List<Entry> referrals) {

    public record Entry(String status, Instant createdAt, Instant qualifiedAt) {
    }
}
