package io.jgitkins.server.domain.exception;

import io.jgitkins.server.domain.error.DomainErrorCode;
import io.jgitkins.server.domain.model.vo.RunnerStatus;

public class RunnerAlreadyActiveException extends DomainException {

    private final Long runnerId;
    private final RunnerStatus currentStatus;

    public RunnerAlreadyActiveException(Long runnerId, RunnerStatus currentStatus) {
        super(DomainErrorCode.RUNNER_ALREADY_ACTIVED,
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
