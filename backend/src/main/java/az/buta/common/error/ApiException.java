package az.buta.common.error;

import java.util.List;
import java.util.Map;

public class ApiException extends RuntimeException {

    private final ErrorCode code;
    private final transient Object[] arguments;
    private final transient List<FieldViolation> violations;
    private final transient Map<String, Object> properties;

    private ApiException(ErrorCode code, Object[] arguments, List<FieldViolation> violations, Map<String, Object> properties) {
        super(code.name());
        this.code = code;
        this.arguments = arguments;
        this.violations = violations;
        this.properties = properties;
    }

    public static ApiException of(ErrorCode code, Object... arguments) {
        return new ApiException(code, arguments, List.of(), Map.of());
    }

    public static ApiException field(ErrorCode code, String field) {
        return new ApiException(code, new Object[0], List.of(new FieldViolation(field, code.name(), null)), Map.of());
    }

    public static ApiException withProperties(ErrorCode code, Map<String, Object> properties) {
        return new ApiException(code, new Object[0], List.of(), properties);
    }

    public static ApiException notFound() {
        return of(ErrorCode.NOT_FOUND);
    }

    public ErrorCode code() {
        return code;
    }

    public Object[] arguments() {
        return arguments;
    }

    public List<FieldViolation> violations() {
        return violations;
    }

    public Map<String, Object> properties() {
        return properties;
    }
}
