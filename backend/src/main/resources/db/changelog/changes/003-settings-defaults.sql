--liquibase formatted sql

--changeset buta:003-01
INSERT INTO business_settings (key, value, updated_at) VALUES
('pricing', '{"paymentFeePercent": 2.50, "paymentFeeFixed": 0.20, "otherVariableCostPercent": 0.00, "otherVariableCostFixed": 0.30, "minMarginAmount": 1.00, "minMarginPercent": 5.00, "lowStockThreshold": 5, "reservationTtlMinutes": 15}'::jsonb, now()),
('loyalty', '{"pointsPerAzn": 1, "pointValueAzn": 0.01, "expectedRedemptionPercent": 70.00, "redemptionStepPoints": 100, "minRedeemPoints": 100, "maxPointsDiscountPercent": 20.00, "awardTrigger": "FULFILLED", "tierWindowMonths": 0, "referral": {"minFirstOrderAmount": 10.00, "maxRewardsPerMonth": 5, "maxRefereeAccountAgeDays": 30}}'::jsonb, now()),
('membership', '{"plusQualifyingSpendAmount": 100.00, "plusQualifyingWindowDays": 180, "earnedPlusDurationDays": 365}'::jsonb, now()),
('recommendation', '{"weights": {"interest": 30, "budget": 20, "value": 15, "loyalty": 15, "discovery": 10, "business": 10}, "signalWeights": {"purchase": 5.0, "explicitPreference": 4.0, "favorite": 3.0, "cartAdd": 2.0, "detailView": 0.5, "search": 0.5}, "minRelevance": 20, "explorationPercent": 15, "decayHalfLifeDays": 60, "eventRetentionDays": 180, "frequencyCap": {"maxServes": 3, "windowDays": 7}, "diversity": {"maxPerPartnerTop10": 2, "maxPerCategoryTop10": 3}, "variants": [{"name": "control", "allocationPercent": 100}]}'::jsonb, now())
ON CONFLICT (key) DO NOTHING;
--rollback DELETE FROM business_settings WHERE key IN ('pricing', 'loyalty', 'membership', 'recommendation');
