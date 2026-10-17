package az.buta.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record OtpVerifyRequest(@NotBlank @Size(max = 254) String email, @NotBlank @Pattern(regexp = "\\d{6}") String code) {
}
