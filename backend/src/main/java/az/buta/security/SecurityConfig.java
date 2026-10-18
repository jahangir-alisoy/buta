package az.buta.security;

import az.buta.auth.SessionService;
import az.buta.common.config.AppProperties;
import az.buta.common.error.ErrorCode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static az.buta.security.SessionAuthenticationFilter.ACCOUNT_ACTIVE;
import static az.buta.security.SessionAuthenticationFilter.ACCOUNT_PROFILE_INCOMPLETE;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final String STRIPE_WEBHOOK = "/api/v1/payments/stripe/webhook";
    private static final String CONTENT_SECURITY_POLICY = "default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'self'";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, SessionService sessionService, SessionCookies cookies,
                                                   ProblemResponseWriter writer, AppProperties properties) throws Exception {
        CookieCsrfTokenRepository csrfRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfRepository.setCookiePath("/");
        csrfRepository.setCookieCustomizer(cookie -> cookie.sameSite(properties.session().cookieSameSite()).secure(properties.session().cookieSecure()));

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource(properties)))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfRepository)
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
                        .ignoringRequestMatchers(STRIPE_WEBHOOK))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .requestCache(cache -> cache.disable())
                .addFilterBefore(new OriginCheckFilter(properties, writer), CsrfFilter.class)
                .addFilterBefore(new SessionAuthenticationFilter(sessionService, cookies), AnonymousAuthenticationFilter.class)
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(CONTENT_SECURITY_POLICY))
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .frameOptions(frame -> frame.deny())
                        .permissionsPolicyHeader(policy -> policy.policy("camera=(), microphone=(), geolocation=()")))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/v1/docs", "/api/v1/docs/**", "/swagger-ui/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/config", "/api/v1/home", "/api/v1/categories",
                                "/api/v1/partners", "/api/v1/partners/*", "/api/v1/offers", "/api/v1/offers/*",
                                "/api/v1/recommendations").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/interactions").permitAll()
                        .requestMatchers(HttpMethod.POST, STRIPE_WEBHOOK).permitAll()
                        .requestMatchers("/api/v1/auth/complete-profile").hasAuthority(ACCOUNT_PROFILE_INCOMPLETE)
                        .requestMatchers("/api/v1/auth/logout", "/api/v1/auth/logout-all").authenticated()
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/api/v1/dev/**").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/**").hasAuthority(ACCOUNT_ACTIVE)
                        .anyRequest().denyAll())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) ->
                                writer.write(request, response, ErrorCode.AUTH_REQUIRED))
                        .accessDeniedHandler((request, response, exception) ->
                                writer.write(request, response, accessDeniedCode(exception))));
        return http.build();
    }

    private static ErrorCode accessDeniedCode(Exception exception) {
        if (exception instanceof CsrfException) {
            return ErrorCode.CSRF_INVALID;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean incomplete = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> ACCOUNT_PROFILE_INCOMPLETE.equals(authority.getAuthority()));
        return incomplete ? ErrorCode.PROFILE_INCOMPLETE : ErrorCode.FORBIDDEN;
    }

    private static CorsConfigurationSource corsConfigurationSource(AppProperties properties) {
        List<String> origins = new ArrayList<>(properties.allowedOrigins());
        origins.add(properties.publicBaseUrl());
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(origins.stream().map(origin -> origin.trim().replaceAll("/+$", "")).filter(origin -> !origin.isEmpty()).toList());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("X-XSRF-TOKEN"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(Duration.ofHours(1));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
