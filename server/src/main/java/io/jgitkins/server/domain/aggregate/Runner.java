package io.jgitkins.server.domain.aggregate;

import io.jgitkins.server.domain.event.RunnerActivatedEvent;
import io.jgitkins.server.domain.exception.RunnerAlreadyActiveException;
import io.jgitkins.server.domain.exception.RunnerTokenMismatchException;
import io.jgitkins.server.domain.exception.RunnerTokenMissingException;
import io.jgitkins.server.domain.model.vo.RunnerScopeType;
import io.jgitkins.server.domain.model.vo.RunnerStatus;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Runner aggregate captures registration metadata so orchestration layers can
 * reason about availability without leaking persistence concerns.
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Runner extends AbstractAggregateRoot<Long> {

    private final Long id;
    private final String token;
    private final String description;
    private final RunnerStatus status;
    private final RunnerScopeType scopeType;
    private final Long scopeTargetId;
    private final String ipAddress;
    private final LocalDateTime lastHeartbeatAt;
    private final LocalDateTime createdAt;

    public static Runner create(String description,
                                RunnerScopeType scopeType,
                                Long scopeTargetId) {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Runner description is required when registering");
        }
        if (scopeType == null) {
            throw new IllegalArgumentException("Runner scope type is required when registering");
        }
        if (scopeType.requiresTargetId() && scopeTargetId == null) {
            throw new IllegalArgumentException("Runner scope target id is required for scope " + scopeType);
        }

        LocalDateTime now = LocalDateTime.now();

        return new Runner(null,
                          generateToken(),
                          description.trim(),
                          RunnerStatus.OFFLINE,
                          scopeType,
                          scopeTargetId,
                          null,
                          now,
                          now);
    }

    private static String generateToken() {
        return "RNR-" + UUID.randomUUID().toString().replaceAll("-", "").substring(0, 24).toUpperCase();
    }

    public Runner withId(Long id) {
        Runner identified = new Runner(id,
                                       token,
                                       description,
                                       status,
                                       scopeType,
                                       scopeTargetId,
                                       ipAddress,
                                       lastHeartbeatAt,
                                       createdAt);
        identified.copyDomainEventsFrom(this);
        return identified;
    }

    public Runner withStatus(RunnerStatus newStatus) {
        Runner updated = new Runner(id,
                                    token,
                                    description,
                                    newStatus,
                                    scopeType,
                                    scopeTargetId,
                                    ipAddress,
                                    lastHeartbeatAt,
                                    createdAt);
        updated.copyDomainEventsFrom(this);
        return updated;
    }

    public Runner withLastHeartbeatAt(LocalDateTime heartbeatAt) {
        Runner heartbeatUpdated = new Runner(id,
                                             token,
                                             description,
                                             status,
                                             scopeType,
                                             scopeTargetId,
                                             ipAddress,
                                             heartbeatAt,
                                             createdAt);
        heartbeatUpdated.copyDomainEventsFrom(this);
        return heartbeatUpdated;
    }

    public Runner activate(String providedToken, String remoteIp) {
        validateToken(providedToken);
        validateActivationState();
        Runner activated = new Runner(id,
                                      token,
                                      description,
                                      RunnerStatus.ONLINE,
                                      scopeType,
                                      scopeTargetId,
                                      normalizeIp(remoteIp),
                                      LocalDateTime.now(),
                                      createdAt);
        activated.copyDomainEventsFrom(this);
        activated.registerEvent(RunnerActivatedEvent.from(activated));
        return activated;
    }

    public static Runner restore(Long id,
                                 String token,
                                 String description,
                                 RunnerStatus status,
                                 RunnerScopeType scopeType,
                                 Long scopeTargetId,
                                 String ipAddress,
                                 LocalDateTime lastHeartbeatAt,
                                 LocalDateTime createdAt) {
        return new Runner(id,
                          token,
                          description,
                          status,
                          scopeType,
                          scopeTargetId,
                          ipAddress,
                          lastHeartbeatAt,
                          createdAt);
    }

    private void validateToken(String providedToken) {
        if (providedToken == null || providedToken.isBlank()) {
            throw new RunnerTokenMissingException();
        }
        if (!token.equals(providedToken)) {
            throw new RunnerTokenMismatchException();
        }
    }

    private void validateActivationState() {
        if (status != RunnerStatus.OFFLINE) {
            throw new RunnerAlreadyActiveException(id, status);
        }
    }

    private String normalizeIp(String remoteIp) {
        if (remoteIp == null || remoteIp.isBlank()) {
            return ipAddress;
        }
        return remoteIp.trim();
    }
}
