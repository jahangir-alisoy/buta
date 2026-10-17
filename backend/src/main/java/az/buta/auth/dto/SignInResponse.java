package az.buta.auth.dto;

import az.buta.user.dto.CurrentUserDto;

public record SignInResponse(String nextStep, CurrentUserDto user) {
}
