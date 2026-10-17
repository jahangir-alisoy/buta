package az.buta.ops;

import az.buta.auth.OtpService;
import az.buta.auth.SessionService;
import az.buta.demo.DemoCatalogRefresher;
import az.buta.membership.MembershipService;
import az.buta.order.OrderLifecycleService;
import az.buta.order.RedemptionService;
import az.buta.pricing.BusinessSettingsService;
import az.buta.recommendation.InteractionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.jobs.enabled", havingValue = "true", matchIfMissing = true)
public class ScheduledJobs {

    private static final Logger LOG = LoggerFactory.getLogger(ScheduledJobs.class);

    private final OrderLifecycleService orders;
    private final RedemptionService redemptions;
    private final MembershipService membership;
    private final SessionService sessions;
    private final OtpService otp;
    private final InteractionService interactions;
    private final BusinessSettingsService settings;
    private final ObjectProvider<DemoCatalogRefresher> demoRefresher;

    public ScheduledJobs(OrderLifecycleService orders, RedemptionService redemptions, MembershipService membership, SessionService sessions,
                         OtpService otp, InteractionService interactions, BusinessSettingsService settings,
                         ObjectProvider<DemoCatalogRefresher> demoRefresher) {
        this.orders = orders;
        this.redemptions = redemptions;
        this.membership = membership;
        this.sessions = sessions;
        this.otp = otp;
        this.interactions = interactions;
        this.settings = settings;
        this.demoRefresher = demoRefresher;
    }

    @Scheduled(fixedDelayString = "PT1M", initialDelayString = "PT30S")
    public void expirePendingOrders() {
        int expired;
        do {
            expired = orders.expireDuePending(50);
            if (expired > 0) {
                LOG.info("Expired {} pending orders", expired);
            }
        } while (expired == 50);
    }

    @Scheduled(fixedDelayString = "PT1H", initialDelayString = "PT2M")
    public void expireRedemptions() {
        int expired = redemptions.expireDue();
        if (expired > 0) {
            LOG.info("Expired {} vouchers", expired);
        }
    }

    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Baku")
    public void dailyMaintenance() {
        LOG.info("Daily maintenance: memberships downgraded={}, sessions purged={}, otp purged={}, interactions purged={}",
                membership.expireMemberships(), sessions.purgeExpired(), otp.purgeOld(),
                interactions.purgeExpired(settings.recommendation().eventRetentionDays()));
        demoRefresher.ifAvailable(DemoCatalogRefresher::refresh);
    }
}
