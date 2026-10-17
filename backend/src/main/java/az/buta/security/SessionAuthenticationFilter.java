package az.buta.security;

import az.buta.auth.SessionService;
import az.buta.user.domain.UserStatus;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SessionAuthenticationFilter extends OncePerRequestFilter {

    public static final String ACCOUNT_ACTIVE = "ACCOUNT_ACTIVE";
    public static final String ACCOUNT_PROFILE_INCOMPLETE = "ACCOUNT_PROFILE_INCOMPLETE";

    private final SessionService sessionService;
    private final SessionCookies cookies;

    public SessionAuthenticationFilter(SessionService sessionService, SessionCookies cookies) {
        this.sessionService = sessionService;
        this.cookies = cookies;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        cookies.read(request)
                .flatMap(sessionService::authenticate)
                .ifPresent(user -> SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(user, null, authorities(user))));
        chain.doFilter(request, response);
    }

    private static List<GrantedAuthority> authorities(AuthenticatedUser user) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        if (user.status() == UserStatus.ACTIVE) {
            authorities.add(new SimpleGrantedAuthority(ACCOUNT_ACTIVE));
            user.roles().forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role.name())));
        } else {
            authorities.add(new SimpleGrantedAuthority(ACCOUNT_PROFILE_INCOMPLETE));
        }
        return authorities;
    }
}
