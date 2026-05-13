package io.jgitkins.server.application.dto;

import io.jgitkins.server.execution.domain.aggregate.Job;

public record DispatchableJob(Job job,
                              Long organizeId,
                              String repositoryClonePath) {
}
