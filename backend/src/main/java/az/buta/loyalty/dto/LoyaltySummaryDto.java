package az.buta.loyalty.dto;

import az.buta.common.money.MoneyDto;

import java.time.Instant;
import java.util.List;

public record LoyaltySummaryDto(String plan, boolean earnsPoints, long availablePoints, long lifetimeEarnedPoints, long redeemedPoints,
                                long tierQualifyingPoints, Tier tier, NextTier nextTier, int tierProgressPercent,
                                QualifyingSpend qualifyingSpend, boolean redemptionBlocked, Instant membershipEndsAt,
                                String membershipSource, List<Tier> tiers) {

    public record Tier(String code, String name, long threshold, String multiplier, List<String> benefits) {
    }

    public record NextTier(String code, String name, long threshold, long pointsNeeded) {
    }

    public record QualifyingSpend(MoneyDto spent, MoneyDto threshold, MoneyDto remaining, int windowDays) {
    }
}
