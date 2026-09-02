package io.jgitkins.server.execution.application.contract;

import java.time.LocalDateTime;

/**
 * A runner as seen by a reader.
 *
 * <p>No token. It used to carry one, and the only consumer was the response mapper, which put it on
 * the wire: GET /api/runners answered every caller, authenticated or not, with the runner
 * authentication tokens. That token is what RunnerManagementController#activateRunner consumes, so reading the
 * list was enough to become a runner and start receiving dispatched jobs.
 *
 * <p>Removed from this contract rather than from the response DTO alone. A secret that stops at the
 * adapter boundary is one mapper away from leaking again; one that never crosses the application
 * boundary cannot. Issuance keeps its own type, {@code RunnerRegistrationResult}, which is where a
 * token legitimately travels exactly once.
 */
public record RunnerDetailResult(
        Long runnerId,
        String description,
        String status,
        LocalDateTime lastHeartbeatAt,
        LocalDateTime registeredAt
) {
}
