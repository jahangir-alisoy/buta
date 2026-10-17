package az.buta.catalog;

import az.buta.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CatalogBrowsingIT extends IntegrationTest {

    @Test
    void anonymousVisitorBrowsesPublicCatalog() throws Exception {
        mvc.perform(get("/api/v1/home")).andExpect(status().isOk()).andExpect(jsonPath("$.categories.length()", is(9)));
        mvc.perform(get("/api/v1/categories")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/partners")).andExpect(status().isOk()).andExpect(jsonPath("$.length()", is(10)));
        mvc.perform(get("/api/v1/partners/lumen-cinema")).andExpect(status().isOk()).andExpect(jsonPath("$.demo", is(true)));
        mvc.perform(get("/api/v1/offers")).andExpect(status().isOk()).andExpect(jsonPath("$.totalItems", greaterThan(20)));
        mvc.perform(get("/api/v1/offers/cinema-and-exhibition-evening"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.offer.sellingPrice.amount", is("28.00")))
                .andExpect(jsonPath("$.offer.referenceRetail.amount", is("35.00")))
                .andExpect(jsonPath("$.offer.savings.amount", is("7.00")))
                .andExpect(jsonPath("$.components.length()", is(2)));
        mvc.perform(get("/api/v1/config")).andExpect(status().isOk()).andExpect(jsonPath("$.paymentMode", is("SANDBOX")));
    }

    @Test
    void searchIsDiacriticInsensitiveAndLocalized() throws Exception {
        mvc.perform(get("/api/v1/offers").param("q", "xezer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].slug", hasItem("textures-of-the-caspian")));
        mvc.perform(get("/api/v1/offers/textures-of-the-caspian").header("Accept-Language", "en"))
                .andExpect(jsonPath("$.offer.title", is("Textures of the Caspian")));
        mvc.perform(get("/api/v1/offers/textures-of-the-caspian").header("Accept-Language", "az"))
                .andExpect(jsonPath("$.offer.title", is("Xəzərin fakturaları")));
    }

    @Test
    void soldOutOffersAreHiddenUnlessRequested() throws Exception {
        mvc.perform(get("/api/v1/offers").param("q", "evening concert"))
                .andExpect(jsonPath("$.totalItems", is(0)));
        mvc.perform(get("/api/v1/offers").param("q", "evening concert").param("includeSoldOut", "true"))
                .andExpect(jsonPath("$.items[0].availability", is("SOLD_OUT")));
    }

    @Test
    void invalidFiltersAreRejected() throws Exception {
        mvc.perform(get("/api/v1/offers").param("sort", "NOPE")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")));
        mvc.perform(get("/api/v1/offers").param("size", "500")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/offers/does-not-exist")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("NOT_FOUND")));
    }
}
