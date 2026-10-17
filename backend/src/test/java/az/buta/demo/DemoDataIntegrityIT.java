package az.buta.demo;

import az.buta.catalog.OfferPricingService;
import az.buta.catalog.PricingContext;
import az.buta.catalog.domain.OfferRepository;
import az.buta.catalog.domain.Offer;
import az.buta.loyalty.domain.LedgerEntryRepository;
import az.buta.loyalty.domain.LoyaltyAccount;
import az.buta.loyalty.domain.LoyaltyAccountRepository;
import az.buta.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DemoDataIntegrityIT extends IntegrationTest {

    @Autowired
    private OfferRepository offers;

    @Autowired
    private OfferPricingService pricing;

    @Autowired
    private LoyaltyAccountRepository accounts;

    @Autowired
    private LedgerEntryRepository ledger;

    @Autowired
    private TransactionTemplate transactions;

    @Test
    void everySeededDemoOfferPassesTheMarginRules() {
        List<String> failing = transactions.execute(status -> {
            PricingContext context = pricing.context();
            return offers.findAll().stream()
                    .filter(offer -> offer.slug().length() > 0 && offer.getId().toString().startsWith("50000000"))
                    .filter(offer -> !pricing.breakdown(offer, context).sellable())
                    .map(Offer::slug)
                    .toList();
        });
        assertThat(failing).isEmpty();
    }

    @Test
    void everyLoyaltyAccountReconcilesWithItsLedger() {
        for (LoyaltyAccount account : accounts.findAll()) {
            assertThat(ledger.sumPoints(account.userId())).as("ledger for %s", account.userId()).isEqualTo(account.availablePoints());
        }
    }

    @Test
    void demoPartnersAreLabeledAsDemo() {
        transactions.executeWithoutResult(status -> offers.findAll().stream()
                .filter(offer -> offer.getId().toString().startsWith("50000000"))
                .flatMap(offer -> offer.components().stream())
                .forEach(component -> assertThat(component.product().partner().demo()).isTrue()));
    }

}
