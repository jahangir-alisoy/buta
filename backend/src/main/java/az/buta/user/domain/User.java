package az.buta.user.domain;

import az.buta.common.domain.AbstractEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "users")
public class User extends AbstractEntity {

    @Column(nullable = false)
    private String email;

    @Column(name = "email_canonical", nullable = false)
    private String emailCanonical;

    private String username;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    @Column(name = "avatar_file_id")
    private UUID avatarFileId;

    @Column(name = "referral_code", nullable = false)
    private String referralCode;

    @Column(name = "registration_ip_hash")
    private String registrationIpHash;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    private Set<RoleGrant> roles = new HashSet<>();

    protected User() {
    }

    public static User register(String email, String referralCode, String registrationIpHash, Instant now) {
        User user = new User();
        user.email = email;
        user.emailCanonical = EmailAddresses.canonical(email);
        user.referralCode = referralCode;
        user.registrationIpHash = registrationIpHash;
        user.status = UserStatus.PROFILE_INCOMPLETE;
        user.createdAt = now;
        user.updatedAt = now;
        user.roles.add(new RoleGrant(Role.CUSTOMER, RoleSource.SYSTEM, now));
        return user;
    }

    public static User registerUnverified(String email, String referralCode, String registrationIpHash, String firstName,
                                          String lastName, String username, Instant now) {
        User user = register(email, referralCode, registrationIpHash, now);
        user.completeProfile(firstName, lastName, username, now);
        return user;
    }

    public boolean markEmailVerified(Instant now) {
        if (emailVerifiedAt != null) {
            return false;
        }
        emailVerifiedAt = now;
        updatedAt = now;
        return true;
    }

    public boolean isEmailVerified() {
        return emailVerifiedAt != null;
    }

    public void completeProfile(String firstName, String lastName, String username, Instant now) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.username = username;
        this.status = UserStatus.ACTIVE;
        this.updatedAt = now;
    }

    public void updateProfile(String firstName, String lastName, String username, String phone, Instant now) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.username = username;
        this.phone = phone;
        this.updatedAt = now;
    }

    public void changeEmail(String newEmail, Instant now) {
        this.email = newEmail;
        this.emailCanonical = EmailAddresses.canonical(newEmail);
        this.updatedAt = now;
    }

    public void changeAvatar(UUID fileId, Instant now) {
        this.avatarFileId = fileId;
        this.updatedAt = now;
    }

    public void suspend(Instant now) {
        this.status = UserStatus.SUSPENDED;
        this.updatedAt = now;
    }

    public void reactivate(Instant now) {
        this.status = username == null ? UserStatus.PROFILE_INCOMPLETE : UserStatus.ACTIVE;
        this.updatedAt = now;
    }

    public boolean grantRole(Role role, RoleSource source, Instant now) {
        return roles.add(new RoleGrant(role, source, now));
    }

    public boolean revokeRole(Role role) {
        return roles.removeIf(grant -> grant.role() == role);
    }

    public boolean hasRole(Role role) {
        return roles.stream().anyMatch(grant -> grant.role() == role);
    }

    public RoleSource roleSource(Role role) {
        return roles.stream().filter(grant -> grant.role() == role).map(RoleGrant::source).findFirst().orElse(null);
    }

    public Set<Role> roleSet() {
        return roles.stream().map(RoleGrant::role).collect(Collectors.toUnmodifiableSet());
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    public String email() {
        return email;
    }

    public String emailCanonical() {
        return emailCanonical;
    }

    public String username() {
        return username;
    }

    public String firstName() {
        return firstName;
    }

    public String lastName() {
        return lastName;
    }

    public String phone() {
        return phone;
    }

    public UserStatus status() {
        return status;
    }

    public UUID avatarFileId() {
        return avatarFileId;
    }

    public String referralCode() {
        return referralCode;
    }

    public String registrationIpHash() {
        return registrationIpHash;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
