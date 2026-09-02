package io.jgitkins.server.execution.application.contract.external;

import io.jgitkins.server.execution.domain.aggregate.Job;

public record DispatchableJob(Long jobId,
                              Job job,
                              Long organizeId,
                              String repositoryClonePath) {
}
