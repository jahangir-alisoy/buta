package az.buta.common.i18n;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.List;
import java.util.Locale;

@Configuration
public class LocaleConfig {

    public static final Locale AZERBAIJANI = Locale.of("az");
    public static final List<Locale> SUPPORTED = List.of(AZERBAIJANI, Locale.ENGLISH);

    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setSupportedLocales(SUPPORTED);
        resolver.setDefaultLocale(AZERBAIJANI);
        return resolver;
    }

    public static Locale normalize(String language) {
        return "en".equalsIgnoreCase(language) ? Locale.ENGLISH : AZERBAIJANI;
    }
}
