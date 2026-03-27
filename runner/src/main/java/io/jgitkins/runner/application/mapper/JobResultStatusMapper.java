package io.jgitkins.runner.application.mapper;

import io.jgitkins.server.grpc.JobResultStatus;

public final class JobResultStatusMapper {

    private JobResultStatusMapper() {
    }

    public static JobResultStatus fromExitCode(int exitCode) {
        return exitCode == 0 ? JobResultStatus.JOB_RESULT_SUCCESS : JobResultStatus.JOB_RESULT_FAILED;
    }
}
