package az.buta.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OtpRequest(@NotBlank @Size(max = 254) String email) {
}
