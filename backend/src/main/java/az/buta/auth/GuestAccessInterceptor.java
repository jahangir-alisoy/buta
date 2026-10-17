package az.buta.auth;

import az.buta.common.error.ApiException;
import az.buta.common.error.ErrorCode;
import az.buta.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

@Component
public class GuestAccessInterceptor implements HandlerInterceptor {

    private static final Set<String> READ_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    private final GuestAccounts guests;

    public GuestAccessInterceptor(GuestAccounts guests) {
        this.guests = guests;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (READ_METHODS.contains(request.getMethod()) || !guests.isGuest(currentUser())) {
            return true;
        }
        String path = request.getRequestURI();
        if (isRestricted(path)) {
            throw ApiException.of(ErrorCode.GUEST_READ_ONLY);
        }
        return true;
    }

    private static boolean isRestricted(String path) {
        boolean ownAccount = (path.equals("/api/v1/me") || path.startsWith("/api/v1/me/")) && !path.startsWith("/api/v1/me/favorites");
        return ownAccount || path.startsWith("/api/v1/admin/") || path.equals("/api/v1/auth/logout-all");
    }

    private static AuthenticatedUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user ? user : null;
    }
}
