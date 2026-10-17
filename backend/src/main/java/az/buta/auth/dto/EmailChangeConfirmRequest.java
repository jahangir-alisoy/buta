package az.buta.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record EmailChangeConfirmRequest(@NotBlank @Pattern(regexp = "\\d{6}") String code) {
}
