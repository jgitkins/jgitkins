package io.jgitkins.server.application.service;

import io.jgitkins.server.application.dto.command.JobCreateCommand;
import io.jgitkins.server.application.port.in.JobCreateUseCase;
import io.jgitkins.server.application.port.out.JobPersistencePort;
import io.jgitkins.server.domain.aggregate.Job;
import io.jgitkins.server.domain.model.vo.BranchName;
import io.jgitkins.server.domain.model.vo.CommitHash;
import io.jgitkins.server.repository.domain.vo.RepositoryId;
import io.jgitkins.server.domain.model.vo.UserId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobService implements JobCreateUseCase {

    private final JobPersistencePort jobPort;

    @Override
    @Transactional
    public void create(JobCreateCommand command) {
        log.info("Creating job for repo: {}, commit: {}, path: {}",
                command.repoName(), command.commitHash(), command.pipelineFilePath());

        Job job = Job.create(RepositoryId.of(command.repositoryId()),
                             CommitHash.of(command.commitHash()),
                             BranchName.of(command.branchName()),
                             UserId.of(command.triggeredBy()));

        jobPort.save(job);

        log.info("Job created successfully. JobId: {}", job.getId());

    }
}
