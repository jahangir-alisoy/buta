package az.buta.pricing;

public enum SettingsKey {
    PRICING("pricing", PricingSettings.class),
    LOYALTY("loyalty", LoyaltySettings.class),
    MEMBERSHIP("membership", MembershipSettings.class),
    RECOMMENDATION("recommendation", RecommendationSettings.class);

    private final String key;
    private final Class<?> type;

    SettingsKey(String key, Class<?> type) {
        this.key = key;
        this.type = type;
    }

    public String key() {
        return key;
    }

    public Class<?> type() {
        return type;
    }

    public static SettingsKey fromKey(String key) {
        for (SettingsKey value : values()) {
            if (value.key.equals(key)) {
                return value;
            }
        }
        throw new IllegalArgumentException(key);
    }
}
