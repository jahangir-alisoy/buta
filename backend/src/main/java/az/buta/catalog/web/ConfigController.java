package az.buta.catalog.web;

import az.buta.common.config.AppProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/config")
public class ConfigController {

    private final AppProperties properties;

    public ConfigController(AppProperties properties) {
        this.properties = properties;
    }

    @GetMapping
    public ClientConfig config() {
        return new ClientConfig(List.of("az", "en"), "az", properties.demoMode(),
                properties.payment().provider().toUpperCase(java.util.Locale.ROOT), "AZN", properties.businessTimeZone().getId());
    }

    public record ClientConfig(List<String> locales, String defaultLocale, boolean demoMode, String paymentMode, String currency,
                               String timeZone) {
    }
}
