package io.jgitkins.server.execution.application.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jgitkins.server.execution.application.contract.result.RunnerDetailResult;
import io.jgitkins.server.execution.application.contract.result.RunnerRegistrationResult;
import io.jgitkins.server.execution.domain.aggregate.Runner;
import io.jgitkins.server.execution.domain.vo.RunnerScopeType;
import io.jgitkins.server.execution.domain.vo.RunnerStatus;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

/**
 * Task 2.124. The runner authentication token must not cross the boundary from the domain into a
 * read result.
 *
 * <p>GET /api/runners and GET /api/runners/{id} took no principal and returned RunnerResponse with a
 * token field. That value is what RunnerController#activateRunner consumes, so an unauthenticated
 * read was enough to become a runner and start receiving dispatched jobs.
 *
 * <p>This tests the MAPPING, not the response shape. Asserting that $.data.token does not exist
 * cannot fail once the field is off the record -- the compiler already prevents that, and a test that
 * cannot fail is not a test. What can silently regress is the mapping: MapStruct matches by name, so
 * reintroducing any token-ish field on the result would refill it from the aggregate without anyone
 * writing a line of mapping code. Serializing and searching for the secret's VALUE catches that
 * whatever the field ends up being called.
 */
class RunnerTokenDoesNotCrossTheReadBoundaryTest {

    private static final String TOKEN = "RNR-SECRETVALUE0123456789AB";

    private final RunnerApplicationMapper mapper = Mappers.getMapper(RunnerApplicationMapper.class);
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private static Runner runnerWithToken() {
        LocalDateTime now = LocalDateTime.now();
        return Runner.restore(1L, TOKEN, "build-01", RunnerStatus.ONLINE,
                RunnerScopeType.GLOBAL, null, "10.0.0.1", now, now);
    }

    @Test
    void theReadResultCarriesNoFieldHoldingTheToken() throws Exception {
        RunnerDetailResult result = mapper.toActivationResult(runnerWithToken());

        assertThat(objectMapper.writeValueAsString(result)).doesNotContain(TOKEN);
    }

    @Test
    void theRegistrationResultStillCarriesIt() throws Exception {
        // The one place a token legitimately travels: issuance, once, to whoever registered the
        // runner. Removing it there would break registration rather than fix a leak.
        RunnerRegistrationResult result = mapper.toRegistrationResult(runnerWithToken());

        assertThat(objectMapper.writeValueAsString(result)).contains(TOKEN);
    }
}
