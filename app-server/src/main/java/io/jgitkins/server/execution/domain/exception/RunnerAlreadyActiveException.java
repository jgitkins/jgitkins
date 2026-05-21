package io.jgitkins.server.execution.domain.exception;

import io.jgitkins.server.shared.domain.error.DomainProblemSpec;
import io.jgitkins.server.shared.domain.exception.DomainException;
import io.jgitkins.server.execution.domain.vo.RunnerStatus;

public class RunnerAlreadyActiveException extends DomainException {

    private final Long runnerId;
    private final RunnerStatus currentStatus;

    public RunnerAlreadyActiveException(Long runnerId, RunnerStatus currentStatus) {
        super(DomainProblemSpec.RUNNER_ALREADY_ACTIVE,
              "Runner " + runnerId + " is already " + currentStatus + " and cannot be activated again");
        this.runnerId = runnerId;
        this.currentStatus = currentStatus;
    }

    public Long getRunnerId() {
        return runnerId;
    }

    public RunnerStatus getCurrentStatus() {
        return currentStatus;
    }
}
