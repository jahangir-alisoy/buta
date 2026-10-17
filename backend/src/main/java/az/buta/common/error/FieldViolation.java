package az.buta.common.error;

public record FieldViolation(String field, String code, String message) {
}
