package az.buta.membership;

import java.time.Instant;
import java.util.Set;

public record MembershipStatus(PlanCode plan, MembershipSource source, Instant startsAt, Instant endsAt,
                               Set<MembershipFeature> features) {

    public boolean has(MembershipFeature feature) {
        return features.contains(feature);
    }

    public boolean isPlus() {
        return plan == PlanCode.PLUS;
    }
}
