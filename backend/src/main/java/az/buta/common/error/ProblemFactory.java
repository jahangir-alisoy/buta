package az.buta.common.error;

import az.buta.common.web.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class ProblemFactory {

    private final MessageSource messageSource;

    public ProblemFactory(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public ProblemDetail create(ErrorCode code, HttpServletRequest request, Object... arguments) {
        return create(code, request, List.of(), Map.of(), arguments);
    }

    public ProblemDetail create(ErrorCode code, HttpServletRequest request, List<FieldViolation> violations,
                                Map<String, Object> properties, Object... arguments) {
        Locale locale = LocaleContextHolder.getLocale();
        ProblemDetail problem = ProblemDetail.forStatus(code.status());
        problem.setDetail(message("error." + code.name(), arguments, locale));
        problem.setProperty("code", code.name());
        if (!violations.isEmpty()) {
            problem.setProperty("errors", violations.stream()
                    .map(violation -> new FieldViolation(violation.field(), violation.code(),
                            violation.message() != null ? violation.message() : message("error." + violation.code(), new Object[0], locale)))
                    .toList());
        }
        properties.forEach(problem::setProperty);
        if (request != null) {
            problem.setProperty("requestId", RequestIdFilter.current(request));
        }
        return problem;
    }

    private String message(String key, Object[] arguments, Locale locale) {
        return messageSource.getMessage(key, arguments, key, locale);
    }
}
