package io.jgitkins.server.execution.application.contract.internal;

import io.jgitkins.server.execution.domain.aggregate.Job;

public record DispatchableJob(Job job,
                              Long organizeId,
                              String repositoryClonePath) {
}
