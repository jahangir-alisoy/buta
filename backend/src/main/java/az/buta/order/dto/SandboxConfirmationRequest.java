package az.buta.order.dto;

import jakarta.validation.constraints.NotNull;

public record SandboxConfirmationRequest(@NotNull Outcome outcome) {

    public enum Outcome {
        SUCCEEDED, FAILED
    }
}
