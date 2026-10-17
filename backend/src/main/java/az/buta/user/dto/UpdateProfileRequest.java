package az.buta.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank @Size(max = 50) String firstName,
        @NotBlank @Size(max = 50) String lastName,
        @NotBlank @Size(min = 3, max = 30) String username,
        @Pattern(regexp = "^\\+[1-9]\\d{7,14}$") String phone) {
}
