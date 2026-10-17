package az.buta.security;

import az.buta.common.config.AppProperties;
import az.buta.common.error.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class OriginCheckFilter extends OncePerRequestFilter {

    private static final Set<String> SAFE_METHODS = Set.of(HttpMethod.GET.name(), HttpMethod.HEAD.name(), HttpMethod.OPTIONS.name());

    private final Set<String> allowedOrigins = new HashSet<>();
    private final ProblemResponseWriter writer;

    public OriginCheckFilter(AppProperties properties, ProblemResponseWriter writer) {
        this.writer = writer;
        allowedOrigins.add(originOf(properties.publicBaseUrl()));
        properties.allowedOrigins().forEach(origin -> allowedOrigins.add(originOf(origin)));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String origin = request.getHeader("Origin");
        if (!SAFE_METHODS.contains(request.getMethod()) && origin != null && !"null".equals(origin)
                && !allowedOrigins.contains(originOf(origin)) && !isSameHost(request, origin)) {
            writer.write(request, response, ErrorCode.ORIGIN_NOT_ALLOWED);
            return;
        }
        if (!SAFE_METHODS.contains(request.getMethod()) && "null".equals(origin)) {
            writer.write(request, response, ErrorCode.ORIGIN_NOT_ALLOWED);
            return;
        }
        chain.doFilter(request, response);
    }

    private static boolean isSameHost(HttpServletRequest request, String origin) {
        try {
            URI uri = URI.create(origin);
            String host = request.getHeader("X-Forwarded-Host") != null ? request.getHeader("X-Forwarded-Host") : request.getHeader("Host");
            return host != null && uri.getAuthority() != null && uri.getAuthority().equalsIgnoreCase(host);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static String originOf(String url) {
        try {
            URI uri = URI.create(url.trim());
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            int port = uri.getPort();
            return port == -1 ? scheme + "://" + host : scheme + "://" + host + ":" + port;
        } catch (IllegalArgumentException exception) {
            return "";
        }
    }
}
