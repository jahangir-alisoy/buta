package az.buta.common.config;

import az.buta.auth.GuestAccessInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final GuestAccessInterceptor guestAccess;

    public WebConfig(GuestAccessInterceptor guestAccess) {
        this.guestAccess = guestAccess;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(guestAccess).addPathPatterns("/api/**");
    }
}
