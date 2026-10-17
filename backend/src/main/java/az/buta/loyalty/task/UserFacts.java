package az.buta.loyalty.task;

import java.time.Instant;
import java.util.List;

public record UserFacts(List<PurchaseFact> purchases, List<Instant> eventAttendances, List<Instant> qualifiedReferrals) {
}
