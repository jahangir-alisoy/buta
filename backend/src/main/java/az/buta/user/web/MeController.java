package az.buta.user.web;

import az.buta.auth.EmailChangeService;
import az.buta.auth.SessionService;
import az.buta.auth.dto.EmailChangeConfirmRequest;
import az.buta.auth.dto.EmailChangeRequest;
import az.buta.auth.dto.OtpRequestResponse;
import az.buta.common.error.ApiException;
import az.buta.security.AuthenticatedUser;
import az.buta.security.SessionCookies;
import az.buta.user.UserAccountService;
import az.buta.user.dto.CurrentUserDto;
import az.buta.user.dto.PreferencesDto;
import az.buta.user.dto.UpdatePreferencesRequest;
import az.buta.user.dto.UpdateProfileRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/me")
public class MeController {

    private final UserAccountService accounts;
    private final EmailChangeService emailChange;
    private final SessionCookies cookies;

    public MeController(UserAccountService accounts, EmailChangeService emailChange, SessionCookies cookies) {
        this.accounts = accounts;
        this.emailChange = emailChange;
        this.cookies = cookies;
    }

    @GetMapping
    public CurrentUserDto me(@AuthenticationPrincipal AuthenticatedUser user) {
        return accounts.currentUser(user.userId());
    }

    @PatchMapping
    public CurrentUserDto update(@AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody UpdateProfileRequest request) {
        return accounts.updateProfile(user.userId(), request);
    }

    @GetMapping("/preferences")
    public PreferencesDto preferences(@AuthenticationPrincipal AuthenticatedUser user) {
        return accounts.preferencesDto(user.userId());
    }

    @PutMapping("/preferences")
    public PreferencesDto updatePreferences(@AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody UpdatePreferencesRequest request) {
        return accounts.updatePreferences(user.userId(), request);
    }

    @GetMapping("/avatar")
    public ResponseEntity<byte[]> avatar(@AuthenticationPrincipal AuthenticatedUser user) {
        byte[] content = accounts.avatar(user.userId()).orElseThrow(ApiException::notFound);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePrivate())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(content);
    }

    @PutMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CurrentUserDto uploadAvatar(@AuthenticationPrincipal AuthenticatedUser user, @RequestPart("file") MultipartFile file) {
        return accounts.replaceAvatar(user.userId(), file);
    }

    @DeleteMapping("/avatar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeAvatar(@AuthenticationPrincipal AuthenticatedUser user) {
        accounts.removeAvatar(user.userId());
    }

    @PostMapping("/email-change")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public OtpRequestResponse requestEmailChange(@AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody EmailChangeRequest request,
                                                 HttpServletRequest http) {
        return new OtpRequestResponse(emailChange.request(user, request.newEmail(), http.getRemoteAddr(), LocaleContextHolder.getLocale()));
    }

    @PostMapping("/email-change/confirm")
    public CurrentUserDto confirmEmailChange(@AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody EmailChangeConfirmRequest request,
                                             HttpServletRequest http, HttpServletResponse response) {
        SessionService.IssuedSession session = emailChange.confirm(user, request.code(), http.getHeader(HttpHeaders.USER_AGENT),
                http.getRemoteAddr(), LocaleContextHolder.getLocale());
        cookies.write(response, session.token());
        return accounts.currentUser(user.userId());
    }
}
