package io.jgitkins.server.common.presentation.advice;

import io.jgitkins.server.shared.application.exception.ApplicationException;
import io.jgitkins.core.common.error.ErrorCode;
import io.jgitkins.core.common.exception.JgitkinsException;
import io.jgitkins.server.shared.domain.exception.DomainException;
import io.jgitkins.server.common.infrastructure.error.InfrastructureErrorCode;
import io.jgitkins.server.common.infrastructure.exception.InfrastructureException;
import io.jgitkins.server.common.presentation.advice.translator.CompositeErrorHttpStatusMapper;
import io.jgitkins.core.web.api.response.ApiResponse;
import io.jgitkins.server.common.presentation.error.PresentationProblemSpec;
import io.jgitkins.server.common.presentation.exception.PresentationException;
import io.jgitkins.server.common.infrastructure.config.security.handler.DeniedRequestLog;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
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

    /**
     * What a caller reads when no resolver below claims the exception.
     *
     * <p>This used to be {@code ex.getMessage()}, which made the DEFAULT "hand Spring's own text to
     * the caller". Three of the six types registered on {@link #handlePresentationException(Exception)}
     * had no branch and reached it, so a type mismatch answered with {@code java.lang.Long}, a missing
     * parameter with {@code for method parameter type String}, and a constraint violation with the
     * handler method's name. The two {@code isBlank()} fallbacks below reached it too.
     *
     * <p>Inverting the default closes the class rather than those three instances: a type nobody
     * wrote a resolver for, a type added to the annotation later, and a violation whose messages are
     * all blank now all answer something safe. {@link #MESSAGE_RESOLVERS} is what makes an answer
     * USEFUL; this constant is what makes the absence of one SAFE.
     */
    private static final String FALLBACK_MESSAGE = "Invalid request";

    /** Said instead of Spring's {@code "No static resource api/..."}, which named the caller's path and called an API route a static file. */
    private static final String NO_ENDPOINT_MESSAGE = "No endpoint matches this request";

    /**
     * Which registered exception type produces which message.
     *
     * <pre>
     *   ex ─┬─ HandlerMethodValidation ────▶ name: message, per parameter   (no @Validated)
     *       ├─ ConstraintViolation ────────▶ name: message, per violation   (@Validated, AOP)
     *       ├─ MethodArgumentNotValid ─────▶ name: message, per field       (request body)
     *       ├─ MethodArgumentTypeMismatch ─▶ name: expected a number
     *       ├─ MissingServletRequestParam ─▶ name: required parameter is missing
     *       ├─ HttpMessageNotReadable ─────▶ "Malformed request body"
     *       └─ (anything else) ────────────▶ FALLBACK_MESSAGE
     * </pre>
     *
     * <p>A table rather than an {@code instanceof} chain for one reason: the chain's branch set lived
     * only inside a method body, so nothing could check it against the annotation's type list, and it
     * drifted to three of six. {@code GlobalExceptionHandlerTest} now compares {@code keySet()} with
     * the annotation and fails when a type is registered without a resolver. Iterated with
     * {@link Class#isInstance} in insertion order, so dispatch matches what the chain did, subclasses
     * included.
     *
     * <p>The first three rows must agree with each other. Spring 6.1 splits parameter validation in
     * two -- a class without {@code @Validated} throws {@code HandlerMethodValidationException}, a
     * class with it goes through AOP and throws {@code ConstraintViolationException} -- and task 2.100
     * deletes the two remaining {@code @Validated} annotations, which moves those routes from the
     * first row to the second. They answer the same shape so that move is a no-op on the wire.
     */
    // Package-private, not private: GlobalExceptionHandlerTest compares this key set against the
    // @ExceptionHandler annotation, and a set it cannot read is a set nothing checks -- which is how
    // the branch count drifted to three of six in the first place.
    static final Map<Class<? extends Exception>, Function<Exception, String>> MESSAGE_RESOLVERS =
            messageResolvers();

    private final CompositeErrorHttpStatusMapper statusMapper;

    private static Map<Class<? extends Exception>, Function<Exception, String>> messageResolvers() {
        Map<Class<? extends Exception>, Function<Exception, String>> resolvers = new LinkedHashMap<>();
        resolvers.put(HandlerMethodValidationException.class,
                ex -> describeErrors(parameterErrors((HandlerMethodValidationException) ex)));
        resolvers.put(ConstraintViolationException.class,
                ex -> describeErrors(violationErrors((ConstraintViolationException) ex)));
        resolvers.put(MethodArgumentNotValidException.class,
                ex -> describeErrors(bodyErrors((MethodArgumentNotValidException) ex)));
        resolvers.put(MethodArgumentTypeMismatchException.class,
                ex -> describeTypeMismatch((MethodArgumentTypeMismatchException) ex));
        resolvers.put(MissingServletRequestParameterException.class,
                ex -> ((MissingServletRequestParameterException) ex).getParameterName()
                        + ": required parameter is missing");
        // Jackson's message carries internal class names and JSON pointers, so the body it could not
        // parse is described rather than quoted.
        resolvers.put(HttpMessageNotReadableException.class, ex -> "Malformed request body");
        return Collections.unmodifiableMap(resolvers);
    }

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

    /**
     * {@code NoResourceFoundException} joined {@code NoHandlerFoundException} here because Spring 6 is
     * what throws for an unmatched path now. Only the older type was listed, so every mistyped URL fell
     * to the {@code Exception} catch-all and answered 500 INTERNAL_ERROR: the server reporting its own
     * failure for what is entirely the caller's typo.
     *
     * <p>The body is a fixed string. {@code ex.getMessage()} was Spring's, which for
     * {@code NoResourceFoundException} reads {@code "No static resource api/repositorie/1."} -- it
     * reflected the caller's path back and told a developer who mistyped an API route to go looking at
     * static file serving. Same idiom as {@code "Malformed request body"}: describe the failure, do not
     * quote the framework.
     *
     * <p>And it logs, which it did not. Every other handler in this class records something, so an
     * unmatched path was the one client mistake that left no trace -- neither a routing bug nor a path
     * scan reached anyone. {@link DeniedRequestLog} is what the security handlers already use for the
     * same problem (a path is caller input, so a raw CR or LF could forge a second log line); it also
     * bounds the line and records only the exception's class name, never its message.
     */
    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ApiResponse<Void>> handleNoHandler(Exception ex, HttpServletRequest request) {
        log.warn("No handler: {}", DeniedRequestLog.describe(request, ex));
        return buildResponse(PresentationProblemSpec.NOT_FOUND, HttpStatus.NOT_FOUND, NO_ENDPOINT_MESSAGE,
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

    /**
     * The message a 400 carries, resolved from {@link #MESSAGE_RESOLVERS} and never from
     * {@code ex.getMessage()}.
     *
     * <p>Spring 6.1 splits parameter validation in two, which is why the first three resolvers exist
     * and must agree: a class without {@code @Validated} gets the built-in path and throws
     * {@code HandlerMethodValidationException}, a class with it goes through AOP and throws
     * {@code ConstraintViolationException}. Both are the same failure to a caller, and both must
     * carry the {@code ApiResponse} envelope -- the built-in one implements {@code ErrorResponse}, so
     * without a resolver Spring answers 400 with an empty body, leaving the caller a status and no
     * code, message, or field name.
     */
    private String extractValidationMessage(Exception ex) {
        for (Map.Entry<Class<? extends Exception>, Function<Exception, String>> resolver
                : MESSAGE_RESOLVERS.entrySet()) {
            if (resolver.getKey().isInstance(ex)) {
                return resolver.getValue().apply(ex);
            }
        }
        return FALLBACK_MESSAGE;
    }

    /**
     * One rejection, with the name the caller used for the thing rejected.
     *
     * <p>{@code name} is nullable on purpose: a parameter name needs {@code -parameters} at compile
     * time, and a constraint path can bottom out on the method rather than an argument. A null name
     * degrades to the bare message rather than printing {@code "null: "}.
     */
    private record NamedError(String name, String message) {
    }

    /**
     * {@code name: message} per error, joined with {@code "; "}.
     *
     * <p>One error keeps the bare constraint message -- that is what the HTTP compatibility tests pin
     * and what a caller with one bad field wants to read; prefixing it would produce
     * "username: username is required". Several get their names, because without them the caller
     * cannot tell which message belongs to which field, and three bad path variables answered
     * "must not be blank; must not be blank; must not be blank".
     *
     * <p>Every error, not just the first. {@code getFieldError()} returns one, which nobody noticed
     * while five DTOs carried constraints; task 2.94 put them on twelve more.
     *
     * <p>No reportable error falls back to {@link #FALLBACK_MESSAGE}, not to {@code ex.getMessage()}.
     */
    private static String describeErrors(List<NamedError> errors) {
        List<NamedError> reportable = errors.stream()
                .filter(error -> error.message() != null && !error.message().isBlank())
                .toList();
        if (reportable.isEmpty()) {
            return FALLBACK_MESSAGE;
        }
        if (reportable.size() == 1) {
            return reportable.get(0).message();
        }
        return reportable.stream()
                .map(error -> error.name() == null
                        ? error.message()
                        : error.name() + ": " + error.message())
                .collect(Collectors.joining("; "));
    }

    /** Path variables and query parameters on a controller WITHOUT {@code @Validated}. */
    private static List<NamedError> parameterErrors(HandlerMethodValidationException ex) {
        return ex.getAllValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new NamedError(nameOf(result), error.getDefaultMessage())))
                .toList();
    }

    private static String nameOf(ParameterValidationResult result) {
        return result.getMethodParameter().getParameterName();
    }

    /**
     * The same rejections on a controller WITH {@code @Validated}, which routes through AOP instead.
     *
     * <p>Sorted by name because {@code getConstraintViolations()} returns a {@code Set}: without this
     * the joined message reorders between runs and no test could pin it. The other two sources are
     * ordered already and keep their own order.
     *
     * <p>The leaf of the property path rather than the path, which reads
     * {@code uploadFile.namespace} -- the handler method's name is not something the caller sent.
     */
    private static List<NamedError> violationErrors(ConstraintViolationException ex) {
        return ex.getConstraintViolations().stream()
                .map(violation -> new NamedError(leafOf(violation.getPropertyPath()), violation.getMessage()))
                .sorted(Comparator.comparing(NamedError::name,
                        Comparator.nullsLast(Comparator.<String>naturalOrder())))
                .toList();
    }

    private static String leafOf(Path propertyPath) {
        String leaf = null;
        for (Path.Node node : propertyPath) {
            leaf = node.getName();
        }
        return leaf;
    }

    /** Request body fields. */
    private static List<NamedError> bodyErrors(MethodArgumentNotValidException ex) {
        return ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new NamedError(error.getField(), error.getDefaultMessage()))
                .toList();
    }

    /**
     * What the caller sent could not become the type the route asked for.
     *
     * <p>Spring's message names both Java types -- {@code Failed to convert value of type
     * 'java.lang.String' to required type 'java.lang.Long'} -- so the response told the caller the
     * server is Java and which classes it uses, in exchange for nothing they could act on. The wire
     * vocabulary says the same thing usefully: which parameter, and what a valid value looks like.
     */
    private static String describeTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ex.getName() + ": " + expectationFor(ex.getRequiredType());
    }

    private static String expectationFor(Class<?> required) {
        if (required == null) {
            return "value could not be read";
        }
        if (Number.class.isAssignableFrom(required) || required == int.class || required == long.class
                || required == short.class || required == byte.class || required == double.class
                || required == float.class) {
            return "expected a number";
        }
        if (required == Boolean.class || required == boolean.class) {
            return "expected true or false";
        }
        if (required == UUID.class) {
            return "expected a uuid";
        }
        if (required.isEnum()) {
            return "expected one of the allowed values";
        }
        // Deliberately vague rather than naming the class. A route asking for a type not listed here
        // is the case to add a line for, not the case to leak a class name in.
        return "value is not in the expected format";
    }

}
