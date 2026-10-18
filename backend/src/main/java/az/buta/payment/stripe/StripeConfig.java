package az.buta.payment.stripe;

import az.buta.common.config.AppProperties;
import com.stripe.StripeClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "app.payment.provider", havingValue = "stripe")
public class StripeConfig {

    @Bean
    public StripeClient stripeClient(AppProperties properties) {
        return new StripeClient(properties.payment().stripe().secretKey());
    }
}
