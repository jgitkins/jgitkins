package io.jgitkins.server.presentation.advice;

import io.jgitkins.server.application.exception.ApplicationException;
import io.jgitkins.server.common.error.ErrorCode;
import io.jgitkins.server.common.exception.JgitkinsException;
import io.jgitkins.server.domain.exception.DomainException;
import io.jgitkins.server.infrastructure.common.error.InfrastructureErrorCode;
import io.jgitkins.server.infrastructure.exception.InfrastructureException;
import io.jgitkins.server.presentation.advice.mapper.CompositeErrorHttpStatusMapper;
import io.jgitkins.server.presentation.common.ApiResponse;
import io.jgitkins.server.presentation.common.error.PresentationErrorCode;
import io.jgitkins.server.presentation.common.error.PresentationProblemSpec;
import io.jgitkins.server.presentation.exception.PresentationException;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

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
            MethodArgumentTypeMismatchException.class
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


    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoHandler(NoHandlerFoundException ex) {
        return buildResponse(PresentationProblemSpec.BAD_REQUEST, HttpStatus.NOT_FOUND, ex.getMessage(),
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
        if (ex instanceof MethodArgumentNotValidException manve) {
            FieldError fieldError = manve.getBindingResult().getFieldError();
            if (fieldError != null
                    && fieldError.getDefaultMessage() != null
                    && !fieldError.getDefaultMessage().isBlank()) {
                return fieldError.getDefaultMessage();
            }
        }
        return ex.getMessage();
    }

}
