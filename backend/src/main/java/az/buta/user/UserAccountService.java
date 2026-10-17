package az.buta.user;

import az.buta.audit.AuditService;
import az.buta.common.config.AppProperties;
import az.buta.common.error.ApiException;
import az.buta.common.error.ErrorCode;
import az.buta.membership.MembershipService;
import az.buta.storage.FileStorage;
import az.buta.storage.StoredFile;
import az.buta.storage.StoredFileRepository;
import az.buta.user.domain.User;
import az.buta.user.domain.UserPreferences;
import az.buta.user.domain.UserPreferencesRepository;
import az.buta.user.domain.UserRepository;
import az.buta.user.domain.Usernames;
import az.buta.user.dto.CurrentUserDto;
import az.buta.user.dto.PreferencesDto;
import az.buta.user.dto.UpdatePreferencesRequest;
import az.buta.user.dto.UpdateProfileRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Clock;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserAccountService {

    private final UserRepository users;
    private final UserPreferencesRepository preferences;
    private final StoredFileRepository files;
    private final FileStorage storage;
    private final AvatarImageProcessor avatarProcessor;
    private final MembershipService membership;
    private final AuditService audit;
    private final AppProperties properties;
    private final Clock clock;

    public UserAccountService(UserRepository users, UserPreferencesRepository preferences, StoredFileRepository files,
                              FileStorage storage, AvatarImageProcessor avatarProcessor, MembershipService membership,
                              AuditService audit, AppProperties properties, Clock clock) {
        this.users = users;
        this.preferences = preferences;
        this.files = files;
        this.storage = storage;
        this.avatarProcessor = avatarProcessor;
        this.membership = membership;
        this.audit = audit;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public User require(UUID userId) {
        return users.findById(userId).orElseThrow(ApiException::notFound);
    }

    @Transactional
    public UserPreferences preferencesOf(UUID userId) {
        return preferences.findById(userId)
                .orElseGet(() -> preferences.save(UserPreferences.defaults(userId, "az", clock.instant())));
    }

    @Transactional(readOnly = true)
    public Optional<UserPreferences> findPreferences(UUID userId) {
        return preferences.findById(userId);
    }

    @Transactional
    public CurrentUserDto currentUser(UUID userId) {
        User user = require(userId);
        UserPreferences prefs = preferencesOf(userId);
        return new CurrentUserDto(user.getId(), user.email(), user.username(), user.firstName(), user.lastName(), user.phone(),
                user.status().name(), user.roleSet().stream().map(Enum::name).collect(Collectors.toSet()),
                user.avatarFileId() == null ? null : "/api/v1/me/avatar?v=" + user.avatarFileId(),
                membership.current(userId).plan().name(), prefs.locale(), prefs.theme().name(),
                prefs.personalizationEnabled(), prefs.personalizationDecidedAt() != null, user.referralCode());
    }

    @Transactional
    public CurrentUserDto updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = require(userId);
        String username = Usernames.normalize(request.username());
        if (!Usernames.isAllowed(username)) {
            throw ApiException.field(ErrorCode.VALIDATION_FAILED, "username");
        }
        if (!username.equals(user.username()) && users.existsByUsername(username)) {
            throw ApiException.field(ErrorCode.USERNAME_TAKEN, "username");
        }
        user.updateProfile(request.firstName().trim(), request.lastName().trim(), username,
                request.phone() == null || request.phone().isBlank() ? null : request.phone(), clock.instant());
        audit.user(userId, "PROFILE_UPDATED", Map.of());
        return currentUser(userId);
    }

    @Transactional
    public PreferencesDto updatePreferences(UUID userId, UpdatePreferencesRequest request) {
        if (request.budgetMin() != null && request.budgetMax() != null && request.budgetMin().compareTo(request.budgetMax()) > 0) {
            throw ApiException.field(ErrorCode.VALIDATION_FAILED, "budgetMin");
        }
        UserPreferences prefs = preferencesOf(userId);
        prefs.update(request.locale(), request.theme(), request.preferredCategoryIds(), request.budgetMin(), request.budgetMax(),
                request.city() == null || request.city().isBlank() ? null : request.city().trim(), clock.instant());
        if (request.personalizationEnabled() != null
                && (prefs.personalizationDecidedAt() == null || request.personalizationEnabled() != prefs.personalizationEnabled())) {
            prefs.decidePersonalization(request.personalizationEnabled(), clock.instant());
            audit.user(userId, "PERSONALIZATION_CHANGED", Map.of("enabled", request.personalizationEnabled()));
        }
        return toDto(prefs);
    }

    @Transactional
    public PreferencesDto preferencesDto(UUID userId) {
        return toDto(preferencesOf(userId));
    }

    @Transactional
    public CurrentUserDto replaceAvatar(UUID userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.of(ErrorCode.FILE_TYPE_NOT_ALLOWED);
        }
        if (file.getSize() > properties.storage().avatarMaxBytes()) {
            throw ApiException.of(ErrorCode.FILE_TOO_LARGE);
        }
        byte[] normalized;
        try {
            normalized = avatarProcessor.normalize(file.getBytes());
        } catch (IOException exception) {
            throw ApiException.of(ErrorCode.FILE_TYPE_NOT_ALLOWED);
        }
        User user = require(userId);
        UUID previous = user.avatarFileId();
        StoredFile stored = storage.store(userId, "AVATAR", "image/jpeg", normalized);
        user.changeAvatar(stored.getId(), clock.instant());
        if (previous != null) {
            files.findById(previous).ifPresent(storage::delete);
        }
        audit.user(userId, "AVATAR_CHANGED", Map.of());
        return currentUser(userId);
    }

    @Transactional
    public void removeAvatar(UUID userId) {
        User user = require(userId);
        UUID previous = user.avatarFileId();
        if (previous != null) {
            user.changeAvatar(null, clock.instant());
            files.findById(previous).ifPresent(storage::delete);
            audit.user(userId, "AVATAR_REMOVED", Map.of());
        }
    }

    @Transactional(readOnly = true)
    public Optional<byte[]> avatar(UUID userId) {
        User user = require(userId);
        if (user.avatarFileId() == null) {
            return Optional.empty();
        }
        return files.findById(user.avatarFileId())
                .filter(file -> file.ownerUserId().equals(userId))
                .map(storage::load);
    }

    private static PreferencesDto toDto(UserPreferences prefs) {
        return new PreferencesDto(prefs.locale(), prefs.theme().name(), prefs.personalizationEnabled(),
                prefs.personalizationDecidedAt() != null, prefs.preferredCategoryIds(), prefs.budgetMin(), prefs.budgetMax(), prefs.city());
    }
}
