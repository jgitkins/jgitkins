package io.jgitkins.server.support;

import io.jgitkins.server.common.presentation.advice.mapper.ApplicationErrorHttpStatusMapper;
import io.jgitkins.server.common.presentation.advice.mapper.CompositeErrorHttpStatusMapper;
import io.jgitkins.server.common.presentation.advice.mapper.DomainErrorHttpStatusMapper;
import io.jgitkins.server.common.presentation.advice.mapper.ErrorHttpStatusMapper;
import io.jgitkins.server.common.presentation.advice.mapper.InfrastructureErrorHttpStatusMapper;
import io.jgitkins.server.common.presentation.advice.mapper.PresentationErrorHttpStatusMapper;
import java.util.List;
import org.springframework.context.annotation.Import;

/**
 * The real error-to-status mapping, for {@code @WebMvcTest} slices that assert status codes.
 *
 * <p>Deliberately not annotated {@code @Configuration}: it is meant to be pulled in explicitly with
 * {@code @Import}, never picked up by a component scan. Same intent as {@link PermissiveSliceSecurityConfig}.
 *
 * <p>Why a slice needs this. {@code GlobalExceptionHandler} is a {@code @RestControllerAdvice}, so a web
 * slice includes it, but its {@code CompositeErrorHttpStatusMapper} dependency is not a web component and
 * is excluded, and the slice then fails to start. Supplying it as a bare {@code @MockBean} starts the
 * slice but makes {@code map} return {@code null}, so the advice cannot build a response and the original
 * exception escapes as a {@code ServletException} -- which is what a status assertion then reports,
 * confusingly, as the wrong status.
 *
 * <p>The mapper graph is five plain components with no infrastructure behind them, so importing the real
 * ones costs nothing and keeps status assertions meaningful. A stubbed mapper makes them vacuous: a test
 * that stubs {@code map} to return FORBIDDEN and then asserts 403 passes no matter what the production
 * mapping says.
 */
@Import({
        CompositeErrorHttpStatusMapper.class,
        ApplicationErrorHttpStatusMapper.class,
        DomainErrorHttpStatusMapper.class,
        InfrastructureErrorHttpStatusMapper.class,
        PresentationErrorHttpStatusMapper.class
})
public class ErrorStatusMappingTestConfig {

    /**
     * The same mapper graph for tests that build {@code GlobalExceptionHandler} by hand instead of
     * going through a slice, which {@code standaloneSetup} forces.
     *
     * <p>One factory rather than a literal list per call site, because the list has already drifted:
     * {@code RunnerControllerTest} and {@code RepositoryContentControllerTest} each built a composite
     * without {@code PresentationErrorHttpStatusMapper}, so a {@code PresentationErrorCode} there fell
     * to {@link CompositeErrorHttpStatusMapper}'s {@code orElse(INTERNAL_SERVER_ERROR)} while production
     * answered 400 or 401. Nothing failed, because neither context throws one today.
     */
    public static CompositeErrorHttpStatusMapper realMapper() {
        return new CompositeErrorHttpStatusMapper(delegates());
    }

    /**
     * Exposed so {@code ErrorStatusMappingCompletenessTest} can compare this list against the mappers
     * that actually exist. Kept here rather than reading it back off
     * {@code CompositeErrorHttpStatusMapper}, which would mean opening a production accessor for a
     * test's benefit.
     */
    public static List<ErrorHttpStatusMapper> delegates() {
        return List.of(
                new DomainErrorHttpStatusMapper(),
                new ApplicationErrorHttpStatusMapper(),
                new InfrastructureErrorHttpStatusMapper(),
                new PresentationErrorHttpStatusMapper());
    }
}
