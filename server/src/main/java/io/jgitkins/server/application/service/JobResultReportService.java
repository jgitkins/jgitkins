package io.jgitkins.server.application.service;

import io.jgitkins.server.application.dto.JobResultStatus;
import io.jgitkins.server.application.dto.command.JobResultReportCommand;
import io.jgitkins.server.execution.application.port.in.JobResultReportUseCase;
import io.jgitkins.server.execution.domain.aggregate.Job;
import io.jgitkins.server.execution.domain.aggregate.Runner;
import io.jgitkins.server.execution.domain.entity.JobHistory;
import io.jgitkins.server.execution.domain.repository.JobRepository;
import io.jgitkins.server.execution.domain.repository.RunnerRepository;
import io.jgitkins.server.execution.domain.vo.RunnerId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobResultReportService implements JobResultReportUseCase {

    private final JobRepository jobRepository;
    private final RunnerRepository runnerRepository;

    @Override
    @Transactional
    public void reportJobResult(JobResultReportCommand command) {
        Runner runner = runnerRepository.findByToken(command.runnerToken())
                                       .orElseThrow(() -> new IllegalArgumentException("Runner not found for token"));

        Job job = jobRepository.findById(command.jobId())
                              .orElseThrow(() -> new IllegalArgumentException("Job not found for id " + command.jobId()));

        JobHistory previousHistory = job.getLatestHistory();
        RunnerId runnerId = RunnerId.of(String.valueOf(runner.getId()));

        if (command.status() == JobResultStatus.SUCCESS) {
            job.completeSuccess(runnerId);
        } else {
            job.completeFailure(runnerId);
        }

        Optional<Long> persistedId = jobRepository.appendHistoryIfCurrent(job, previousHistory);
        if (persistedId.isEmpty()) {
            throw new IllegalStateException("Failed to persist job result history for job " + command.jobId());
        }
    }
}
