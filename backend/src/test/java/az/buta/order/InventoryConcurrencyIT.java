package az.buta.order;

import az.buta.cart.AddCartItemRequest;
import az.buta.cart.CartService;
import az.buta.catalog.ViewerResolver;
import az.buta.catalog.admin.AdminCatalogService;
import az.buta.catalog.admin.ComponentRequest;
import az.buta.catalog.admin.OfferRequest;
import az.buta.catalog.admin.ProductRequest;
import az.buta.catalog.domain.ActiveStatus;
import az.buta.catalog.domain.CostModel;
import az.buta.catalog.domain.OfferStatus;
import az.buta.catalog.domain.OfferType;
import az.buta.catalog.domain.OfferVisibility;
import az.buta.catalog.domain.ProductInventoryRepository;
import az.buta.catalog.domain.ProductType;
import az.buta.catalog.domain.RedemptionMethod;
import az.buta.common.error.ApiException;
import az.buta.common.i18n.LocalizedText;
import az.buta.security.AuthenticatedUser;
import az.buta.support.IntegrationTest;
import az.buta.user.domain.Role;
import az.buta.user.domain.User;
import az.buta.user.domain.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

class InventoryConcurrencyIT extends IntegrationTest {

    private static final UUID ADMIN_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");
    private static final UUID PARTNER_ID = UUID.fromString("20000000-0000-0000-0000-000000000003");
    private static final UUID MUSEUM_CATEGORY = UUID.fromString("10000000-0000-0000-0000-000000000005");

    @Autowired
    private AdminCatalogService admin;

    @Autowired
    private CartService carts;

    @Autowired
    private CheckoutService checkout;

    @Autowired
    private ViewerResolver viewers;

    @Autowired
    private ProductInventoryRepository inventory;

    @Test
    void concurrentCheckoutsNeverOversell() throws Exception {
        Instant now = Instant.now();
        UUID productId = admin.createProduct(ADMIN_ID, new ProductRequest(PARTNER_ID, MUSEUM_CATEGORY, ProductType.ADMISSION,
                text("Test giriş"), text("Test"), text("Şərtlər"), text("Kodu göstərin"), null, new BigDecimal("20.00"), CostModel.WHOLESALE,
                new BigDecimal("10.00"), null, now.minus(Duration.ofDays(1)), now.plus(Duration.ofDays(30)), null, null, null, null, "Baku",
                RedemptionMethod.VOUCHER_CODE, true, 24, true, ActiveStatus.ACTIVE, 5)).id();
        UUID offerId = admin.createOffer(ADMIN_ID, new OfferRequest(null, OfferType.SINGLE, MUSEUM_CATEGORY,
                new LocalizedText("Yarış testi " + UUID.randomUUID(), "Race test " + UUID.randomUUID()), text("Alt"), text("Təsvir"),
                null, new BigDecimal("18.00"), OfferVisibility.PUBLIC, now.minus(Duration.ofHours(1)), now.plus(Duration.ofDays(20)),
                1, 0, false, List.of(), List.of(new ComponentRequest(productId, 1, null)))).id();
        admin.changeStatus(ADMIN_ID, offerId, OfferStatus.PUBLISHED);
        offerCatalog.evict();

        List<AuthenticatedUser> buyers = new ArrayList<>();
        for (int index = 0; index < 20; index++) {
            User user = User.register(uniqueEmail("race"), "R" + UUID.randomUUID().toString().substring(0, 7).toUpperCase(), null, now);
            user.completeProfile("Race", "Buyer", "race" + UUID.randomUUID().toString().substring(0, 8), now);
            users.save(user);
            AuthenticatedUser principal = new AuthenticatedUser(user.getId(), UUID.randomUUID(), UserStatus.ACTIVE, Set.of(Role.CUSTOMER));
            carts.add(viewers.resolve(principal), new AddCartItemRequest(offerId, 1, null));
            buyers.add(principal);
        }

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(20);
        List<Future<Boolean>> results = new ArrayList<>();
        for (AuthenticatedUser buyer : buyers) {
            results.add(pool.submit(() -> {
                start.await();
                try {
                    checkout.place(viewers.resolve(buyer), 0, UUID.randomUUID().toString());
                    return true;
                } catch (ApiException exception) {
                    return false;
                }
            }));
        }
        start.countDown();
        int succeeded = 0;
        for (Future<Boolean> result : results) {
            if (result.get()) {
                succeeded++;
            }
        }
        pool.shutdown();

        assertThat(succeeded).isEqualTo(5);
        var stock = inventory.findById(productId).orElseThrow();
        assertThat(stock.reservedQuantity()).isEqualTo(5);
        assertThat(stock.available()).isZero();
    }

    private static LocalizedText text(String value) {
        return new LocalizedText(value, value);
    }
}
