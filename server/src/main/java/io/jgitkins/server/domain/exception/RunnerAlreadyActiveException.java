package io.jgitkins.server.domain.exception;

import io.jgitkins.server.domain.error.DomainProblemSpec;
import io.jgitkins.server.domain.model.vo.RunnerStatus;

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
