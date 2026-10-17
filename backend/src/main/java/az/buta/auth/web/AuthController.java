package az.buta.auth.web;

import az.buta.auth.GuestSignInService;
import az.buta.auth.RegistrationService;
import az.buta.auth.SessionService;
import az.buta.auth.SignInService;
import az.buta.auth.dto.CompleteProfileRequest;
import az.buta.auth.dto.GuestSignInRequest;
import az.buta.auth.dto.OtpRequest;
import az.buta.auth.dto.OtpRequestResponse;
import az.buta.auth.dto.OtpVerifyRequest;
import az.buta.auth.dto.RegisterRequest;
import az.buta.auth.dto.SessionResponse;
import az.buta.auth.dto.SignInResponse;
import az.buta.auth.dto.UsernameAvailability;
import az.buta.security.AuthenticatedUser;
import az.buta.security.SessionCookies;
import az.buta.user.UserAccountService;
import az.buta.user.dto.CurrentUserDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final SignInService signIn;
    private final RegistrationService registration;
    private final GuestSignInService guestSignIn;
    private final SessionService sessions;
    private final SessionCookies cookies;
    private final UserAccountService accounts;

    public AuthController(SignInService signIn, RegistrationService registration, GuestSignInService guestSignIn, SessionService sessions,
                          SessionCookies cookies, UserAccountService accounts) {
        this.signIn = signIn;
        this.registration = registration;
        this.guestSignIn = guestSignIn;
        this.sessions = sessions;
        this.cookies = cookies;
        this.accounts = accounts;
    }

    @GetMapping("/session")
    public SessionResponse session(@AuthenticationPrincipal AuthenticatedUser user) {
        return user == null ? new SessionResponse(false, null) : new SessionResponse(true, accounts.currentUser(user.userId()));
    }

    @PostMapping("/otp/request")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public OtpRequestResponse requestCode(@Valid @RequestBody OtpRequest request, HttpServletRequest http) {
        return new OtpRequestResponse(signIn.requestCode(request.email(), http.getRemoteAddr(), LocaleContextHolder.getLocale()));
    }

    @PostMapping("/otp/verify")
    public SignInResponse verify(@Valid @RequestBody OtpVerifyRequest request, HttpServletRequest http, HttpServletResponse response) {
        SignInService.SignInResult result = signIn.verify(request.email(), request.code(), http.getHeader(HttpHeaders.USER_AGENT),
                http.getRemoteAddr(), LocaleContextHolder.getLocale());
        cookies.write(response, result.sessionToken());
        return result.response();
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public SignInResponse register(@Valid @RequestBody RegisterRequest request, HttpServletRequest http, HttpServletResponse response) {
        SignInService.SignInResult result = registration.register(request, http.getHeader(HttpHeaders.USER_AGENT), http.getRemoteAddr(),
                LocaleContextHolder.getLocale());
        cookies.write(response, result.sessionToken());
        return result.response();
    }

    @PostMapping("/guest")
    public SignInResponse guest(@Valid @RequestBody GuestSignInRequest request, HttpServletRequest http, HttpServletResponse response) {
        SignInService.SignInResult result = guestSignIn.signIn(request.kind(), http.getHeader(HttpHeaders.USER_AGENT), http.getRemoteAddr());
        cookies.write(response, result.sessionToken());
        return result.response();
    }

    @PostMapping("/complete-profile")
    public CurrentUserDto completeProfile(@AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody CompleteProfileRequest request) {
        return signIn.completeProfile(user, request);
    }

    @GetMapping("/username-availability")
    public UsernameAvailability usernameAvailability(@RequestParam @NotBlank @Size(max = 30) String username) {
        return new UsernameAvailability(username, signIn.isUsernameAvailable(username));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@AuthenticationPrincipal AuthenticatedUser user, HttpServletResponse response) {
        sessions.revoke(user.sessionId(), "LOGOUT");
        cookies.clear(response);
    }

    @PostMapping("/logout-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logoutAll(@AuthenticationPrincipal AuthenticatedUser user, HttpServletResponse response) {
        sessions.revokeAll(user.userId(), "LOGOUT_ALL");
        cookies.clear(response);
    }
}
