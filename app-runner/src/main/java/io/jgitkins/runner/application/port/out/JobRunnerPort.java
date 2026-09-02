package io.jgitkins.runner.application.port.out;

import io.jgitkins.runner.application.contract.JobRunContext;

public interface JobRunnerPort {
    int run(JobRunContext context) throws InterruptedException;
}
