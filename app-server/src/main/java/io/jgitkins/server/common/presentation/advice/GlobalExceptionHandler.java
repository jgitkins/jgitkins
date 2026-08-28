package io.jgitkins.server.common.presentation.advice;

import io.jgitkins.server.shared.application.exception.ApplicationException;
import io.jgitkins.core.common.error.ErrorCode;
import io.jgitkins.core.common.exception.JgitkinsException;
import io.jgitkins.server.shared.domain.exception.DomainException;
import io.jgitkins.server.common.infrastructure.error.InfrastructureErrorCode;
import io.jgitkins.server.common.infrastructure.exception.InfrastructureException;
import io.jgitkins.server.common.presentation.advice.mapper.CompositeErrorHttpStatusMapper;
import io.jgitkins.core.web.api.response.ApiResponse;
import io.jgitkins.server.common.presentation.error.PresentationErrorCode;
import io.jgitkins.server.common.presentation.error.PresentationProblemSpec;
import io.jgitkins.server.common.presentation.exception.PresentationException;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private static final String SOURCE_PRESENTATION = "presentation";
    private static final String SOURCE_APPLICATION = "application";
    private static final String SOURCE_DOMAIN = "domain";
    private static final String SOURCE_INFRASTRUCTURE = "infrastructure";

    private final CompositeErrorHttpStatusMapper statusMapper;

    // Presentation (Spring MVC / Validation specific)
    @ExceptionHandler({
            ConstraintViolationException.class,
            MethodArgumentNotValidException.class,
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            HandlerMethodValidationException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<ApiResponse<Void>> handlePresentationException(Exception ex) {
        String message = extractValidationMessage(ex);
        log.warn("Presentation exception errorCode=[{}], message=[{}]",
                PresentationProblemSpec.BAD_REQUEST.getCode(), message);
        return buildResponse(PresentationProblemSpec.BAD_REQUEST, HttpStatus.BAD_REQUEST, message, SOURCE_PRESENTATION);
    }

    @ExceptionHandler(PresentationException.class)
    public ResponseEntity<ApiResponse<Void>> handlePresentationException(PresentationException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        HttpStatus status = statusMapper.map(errorCode);
        log.warn("Presentation exception errorCode=[{}], status=[{}], message=[{}]",
                ex.getProblemCode(), status, ex.getMessage());
        return buildResponse(ex, status, SOURCE_PRESENTATION);
    }

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ApiResponse<Void>> handleApplicationException(ApplicationException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        HttpStatus status = statusMapper.map(errorCode);
        log.warn("Application exception errorCode=[{}], status=[{}], message=[{}]",
                ex.getProblemCode(), status, ex.getMessage());
        return buildResponse(ex, status, SOURCE_APPLICATION);
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiResponse<Void>> handleDomainException(DomainException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        HttpStatus status = statusMapper.map(errorCode);
        log.warn("Domain exception errorCode=[{}], status=[{}], message=[{}]",
                ex.getProblemCode(), status, ex.getMessage());
        return buildResponse(ex, status, SOURCE_DOMAIN);
    }

    @ExceptionHandler(InfrastructureException.class)
    public ResponseEntity<ApiResponse<Void>> handleInfrastructureException(InfrastructureException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        HttpStatus status = statusMapper.map(errorCode);
        log.error("Infrastructure exception errorCode=[{}], status=[{}], message=[{}]",
                ex.getProblemCode(), status, ex.getMessage(), ex);
        return buildResponse(ex, status, SOURCE_INFRASTRUCTURE);
    }


    /**
     * {@code NoResourceFoundException} joined {@code NoHandlerFoundException} here because Spring 6 is
     * what throws for an unmatched path now. Only the older type was listed, so every mistyped URL fell
     * to the {@code Exception} catch-all and answered 500 INTERNAL_ERROR: the server reporting its own
     * failure for what is entirely the caller's typo.
     */
    /**
     * Client mistakes in method, content type, or route are answered with the status that names the
     * mistake. All four used to fall to the {@code Exception} catch-all and answer 500 INTERNAL_ERROR:
     * the server reporting its own failure for something entirely on the caller's side. Measured, not
     * assumed -- a probe over the standard Spring MVC client errors found method, media type, and
     * missing-parameter all answering 500.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        log.warn("Method not allowed: {}", ex.getMessage());
        return buildResponse(PresentationProblemSpec.METHOD_NOT_ALLOWED, HttpStatus.METHOD_NOT_ALLOWED,
                ex.getMessage(), SOURCE_PRESENTATION);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex) {
        log.warn("Unsupported media type: {}", ex.getMessage());
        return buildResponse(PresentationProblemSpec.UNSUPPORTED_MEDIA_TYPE,
                HttpStatus.UNSUPPORTED_MEDIA_TYPE, ex.getMessage(), SOURCE_PRESENTATION);
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ApiResponse<Void>> handleNoHandler(Exception ex) {
        return buildResponse(PresentationProblemSpec.NOT_FOUND, HttpStatus.NOT_FOUND, ex.getMessage(),
                SOURCE_PRESENTATION);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception ex) {
        log.error("Unexpected exception", ex);
        return buildResponse(InfrastructureErrorCode.INTERNAL_ERROR,
                HttpStatus.INTERNAL_SERVER_ERROR,
                null,
                SOURCE_INFRASTRUCTURE);
    }

    private ResponseEntity<ApiResponse<Void>> buildResponse(JgitkinsException exception,
            HttpStatus status,
            String source) {
        String responseMessage = resolveMessage(exception, exception.getMessage());
        return ApiResponse.error(status, exception.getProblemCode(), responseMessage, source);
    }

    private ResponseEntity<ApiResponse<Void>> buildResponse(ErrorCode errorCode,
            HttpStatus status,
            String message,
            String source) {
        String responseMessage = (message == null || message.isBlank())
                ? errorCode.getDefaultMessage()
                : message;
        return ApiResponse.error(status, errorCode, responseMessage, source);
    }

    private ResponseEntity<ApiResponse<Void>> buildResponse(PresentationProblemSpec problemSpec,
            HttpStatus status,
            String message,
            String source) {
        String responseMessage = resolveMessage(problemSpec, message);
        return ApiResponse.error(status, problemSpec.getCode(), responseMessage, source);
    }

    private String resolveMessage(JgitkinsException exception, String message) {
        if (message == null || message.isBlank()) {
            return exception.getDefaultMessage();
        }
        return message;
    }

    private String resolveMessage(PresentationProblemSpec problemSpec, String message) {
        if (message == null || message.isBlank()) {
            return problemSpec.getDefaultMessage();
        }
        return message;
    }

    private String extractValidationMessage(Exception ex) {
        // Spring 6.1 splits parameter validation in two. A class without @Validated gets the built-in
        // path and throws HandlerMethodValidationException; a class with it goes through AOP and throws
        // ConstraintViolationException. Both are the same class of failure to a caller, and both must
        // carry the ApiResponse envelope: the built-in one implements ErrorResponse, so without this
        // branch Spring already answered 400 but with an empty body, leaving the caller a status and no
        // error code, message, or field name.
        if (ex instanceof HandlerMethodValidationException hmve) {
            // Every error, for the same reason as the body path below: this one aggregates path
            // variables and body together, so a request with two bad fields reported one and left the
            // caller to discover the rest a round trip at a time.
            String joined = hmve.getAllErrors().stream()
                    .map(MessageSourceResolvable::getDefaultMessage)
                    .filter(message -> message != null && !message.isBlank())
                    .collect(Collectors.joining("; "));
            return joined.isBlank() ? ex.getMessage() : joined;
        }
        // Every field error, not just the first. getFieldError() returns one, which nobody noticed
        // while five DTOs carried constraints; task 2.94 put them on twelve more, so a request with
        // three bad fields would have had the caller fixing them one round trip at a time.
        if (ex instanceof MethodArgumentNotValidException manve) {
            List<FieldError> fieldErrors = manve.getBindingResult().getFieldErrors();
            // One error keeps the bare constraint message, which is what the HTTP compatibility tests
            // pin and what a caller with one bad field wants to read. Prefixing it would produce
            // "username: username is required". Several errors get their field names, because without
            // them the caller cannot tell which message belongs to which field.
            if (fieldErrors.size() == 1) {
                String only = fieldErrors.get(0).getDefaultMessage();
                if (only != null && !only.isBlank()) {
                    return only;
                }
            }
            String joined = fieldErrors.stream()
                    .map(error -> error.getField() + ": " + error.getDefaultMessage())
                    .filter(message -> !message.isBlank())
                    .collect(Collectors.joining("; "));
            if (!joined.isBlank()) {
                return joined;
            }
        }
        // A fixed message for a body we could not parse. ex.getMessage() here is Jackson's, and it
        // carries internal class names and JSON pointers into the response.
        if (ex instanceof HttpMessageNotReadableException) {
            return "Malformed request body";
        }
        return ex.getMessage();
    }

}
