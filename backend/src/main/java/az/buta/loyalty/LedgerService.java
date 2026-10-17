package az.buta.loyalty;

import az.buta.loyalty.domain.LedgerEntry;
import az.buta.loyalty.domain.LedgerEntryRepository;
import az.buta.loyalty.domain.LedgerEntryType;
import az.buta.loyalty.domain.LedgerSourceType;
import az.buta.loyalty.domain.LoyaltyAccount;
import az.buta.loyalty.domain.LoyaltyAccountRepository;
import az.buta.loyalty.domain.LoyaltyTier;
import az.buta.loyalty.domain.LoyaltyTierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class LedgerService {

    private final LedgerEntryRepository entries;
    private final LoyaltyAccountRepository accounts;
    private final LoyaltyTierRepository tiers;
    private final Clock clock;

    public LedgerService(LedgerEntryRepository entries, LoyaltyAccountRepository accounts, LoyaltyTierRepository tiers, Clock clock) {
        this.entries = entries;
        this.accounts = accounts;
        this.tiers = tiers;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public LoyaltyAccount lockAccount(UUID userId) {
        String baseTier = tiers.findAllByOrderByThresholdPointsAsc().getFirst().code();
        accounts.insertIfAbsent(userId, baseTier, clock.instant());
        return accounts.findForUpdate(userId).orElseThrow();
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Optional<LedgerEntry> post(PostingRequest request) {
        if (request.points() == 0) {
            return Optional.empty();
        }
        LoyaltyAccount account = lockAccount(request.userId());
        if (entries.findByIdempotencyKey(request.idempotencyKey()).isPresent()) {
            return Optional.empty();
        }
        LedgerEntryType reversedType = null;
        if (request.reversesEntryId() != null) {
            reversedType = entries.findById(request.reversesEntryId()).map(LedgerEntry::entryType).orElse(null);
        }
        Instant now = clock.instant();
        LedgerEntry entry = entries.save(new LedgerEntry(request.userId(), request.type(), request.points(), request.idempotencyKey(),
                request.sourceType(), request.sourceId(), request.reversesEntryId(), request.descriptionKey(), request.params(),
                request.actorUserId(), now));
        account.apply(request.type(), request.points(), reversedType, now);
        account.assignTier(tierFor(account.tierQualifyingPoints()).code());
        return Optional.of(entry);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Optional<LedgerEntry> reverse(String originalKey, String descriptionKey, UUID actorUserId) {
        return entries.findByIdempotencyKey(originalKey)
                .flatMap(original -> post(new PostingRequest(original.userId(), LedgerEntryType.REVERSAL, -original.points(),
                        "REVERSAL:" + original.getId(), original.sourceType(), original.sourceId(), original.getId(), descriptionKey,
                        original.descriptionParams(), actorUserId)));
    }

    @Transactional(readOnly = true)
    public Optional<LedgerEntry> find(String idempotencyKey) {
        return entries.findByIdempotencyKey(idempotencyKey);
    }

    public LoyaltyTier tierFor(long qualifyingPoints) {
        List<LoyaltyTier> ordered = tiers.findAllByOrderByThresholdPointsAsc();
        LoyaltyTier current = ordered.getFirst();
        for (LoyaltyTier tier : ordered) {
            if (qualifyingPoints >= tier.thresholdPoints()) {
                current = tier;
            }
        }
        return current;
    }

    public record PostingRequest(UUID userId, LedgerEntryType type, long points, String idempotencyKey, LedgerSourceType sourceType,
                                 UUID sourceId, UUID reversesEntryId, String descriptionKey, Map<String, String> params,
                                 UUID actorUserId) {
    }
}
