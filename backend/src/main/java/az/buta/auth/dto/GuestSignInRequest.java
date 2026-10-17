package az.buta.auth.dto;

import az.buta.auth.GuestKind;
import jakarta.validation.constraints.NotNull;

public record GuestSignInRequest(@NotNull GuestKind kind) {
}
