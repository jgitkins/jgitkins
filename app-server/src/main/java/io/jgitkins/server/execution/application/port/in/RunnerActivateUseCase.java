package io.jgitkins.server.execution.application.port.in;

import io.jgitkins.server.execution.application.contract.RunnerActivateResult;

public interface RunnerActivateUseCase {
    //    RunnerDetailResult activate(Long runnerId, String token, String remoteIp);
    RunnerActivateResult activate(String token, String remoteIp);
}
