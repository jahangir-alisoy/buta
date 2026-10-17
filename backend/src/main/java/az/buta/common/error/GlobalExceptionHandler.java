package az.buta.common.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ProblemFactory problems;
    private final MessageSource messageSource;

    public GlobalExceptionHandler(ProblemFactory problems, MessageSource messageSource) {
        this.problems = problems;
        this.messageSource = messageSource;
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ProblemDetail> handleApi(ApiException exception, HttpServletRequest request) {
        return respond(problems.create(exception.code(), request, exception.violations(), exception.properties(), exception.arguments()));
    }

    @ExceptionHandler(RateLimitedException.class)
    public ResponseEntity<ProblemDetail> handleRateLimited(RateLimitedException exception, HttpServletRequest request) {
        long seconds = Math.max(1, exception.retryAfter().toSeconds());
        ProblemDetail problem = problems.create(exception.code(), request, List.of(), Map.of("retryAfterSeconds", seconds));
        return ResponseEntity.status(problem.getStatus())
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(seconds))
                .body(problem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleInvalid(MethodArgumentNotValidException exception, HttpServletRequest request) {
        List<FieldViolation> violations = exception.getBindingResult().getFieldErrors().stream()
                .map(this::toViolation)
                .toList();
        return respond(problems.create(ErrorCode.VALIDATION_FAILED, request, violations, Map.of()));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ProblemDetail> handleMethodValidation(HandlerMethodValidationException exception, HttpServletRequest request) {
        List<FieldViolation> violations = exception.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new FieldViolation(result.getMethodParameter().getParameterName(),
                                error.getCodes() == null ? "INVALID" : lastCode(error.getCodes()),
                                messageSource.getMessage(error, LocaleContextHolder.getLocale()))))
                .toList();
        return respond(problems.create(ErrorCode.VALIDATION_FAILED, request, violations, Map.of()));
    }

    @ExceptionHandler({ConstraintViolationException.class, HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestPartException.class, HttpMediaTypeNotSupportedException.class})
    public ResponseEntity<ProblemDetail> handleBadRequest(Exception exception, HttpServletRequest request) {
        return respond(problems.create(ErrorCode.VALIDATION_FAILED, request));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ProblemDetail> handleMissingHeader(MissingRequestHeaderException exception, HttpServletRequest request) {
        ErrorCode code = "Idempotency-Key".equalsIgnoreCase(exception.getHeaderName())
                ? ErrorCode.IDEMPOTENCY_KEY_REQUIRED
                : ErrorCode.VALIDATION_FAILED;
        return respond(problems.create(code, request));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ProblemDetail> handleUploadSize(MaxUploadSizeExceededException exception, HttpServletRequest request) {
        return respond(problems.create(ErrorCode.FILE_TOO_LARGE, request));
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetail> handleOptimisticLock(ObjectOptimisticLockingFailureException exception, HttpServletRequest request) {
        return respond(problems.create(ErrorCode.CONFLICT, request));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> handleNoResource(NoResourceFoundException exception, HttpServletRequest request) {
        return respond(problems.create(ErrorCode.NOT_FOUND, request));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleMethod(HttpRequestMethodNotSupportedException exception, HttpServletRequest request) {
        return respond(problems.create(ErrorCode.METHOD_NOT_ALLOWED, request));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException exception, HttpServletRequest request) {
        return respond(problems.create(ErrorCode.FORBIDDEN, request));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception exception, HttpServletRequest request) {
        LOG.error("Unhandled exception for {} {}", request.getMethod(), request.getRequestURI(), exception);
        return respond(problems.create(ErrorCode.INTERNAL_ERROR, request));
    }

    private FieldViolation toViolation(FieldError error) {
        String code = error.getCodes() == null ? "INVALID" : lastCode(error.getCodes());
        return new FieldViolation(error.getField(), code, messageSource.getMessage(error, LocaleContextHolder.getLocale()));
    }

    private static String lastCode(String[] codes) {
        return codes[codes.length - 1];
    }

    private static ResponseEntity<ProblemDetail> respond(ProblemDetail problem) {
        return ResponseEntity.status(problem.getStatus()).body(problem);
    }
}
