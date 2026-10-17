package az.buta.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmailChangeRequest(@NotBlank @Size(max = 254) String newEmail) {
}
