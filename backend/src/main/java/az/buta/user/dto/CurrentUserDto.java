package az.buta.user.dto;

import java.util.Set;
import java.util.UUID;

public record CurrentUserDto(UUID id, String email, String username, String firstName, String lastName, String phone,
                             String status, Set<String> roles, String avatarUrl, String membership, String locale,
                             String theme, boolean personalizationEnabled, boolean personalizationDecided, String referralCode,
                             boolean emailVerified) {
}
