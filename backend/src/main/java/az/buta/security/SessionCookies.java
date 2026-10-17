package az.buta.security;

import az.buta.common.config.AppProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import java.time.Duration;
import java.util.Optional;

@Component
public class SessionCookies {

    private final AppProperties.Session settings;

    public SessionCookies(AppProperties properties) {
        this.settings = properties.session();
    }

    public Optional<String> read(HttpServletRequest request) {
        return Optional.ofNullable(WebUtils.getCookie(request, settings.cookieName()))
                .map(jakarta.servlet.http.Cookie::getValue)
                .filter(value -> !value.isBlank());
    }

    public void write(HttpServletResponse response, String token) {
        response.addHeader(HttpHeaders.SET_COOKIE, base(token).maxAge(settings.absoluteTimeout()).build().toString());
    }

    public void clear(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, base("").maxAge(Duration.ZERO).build().toString());
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(settings.cookieName(), value)
                .httpOnly(true)
                .secure(settings.cookieSecure())
                .sameSite(settings.cookieSameSite())
                .path("/");
    }
}
