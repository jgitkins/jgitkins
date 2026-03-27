package io.jgitkins.runner.application.service;

import io.jgitkins.runner.application.mapper.JobResultStatusMapper;
import io.jgitkins.runner.application.port.in.RunnerJobUseCase;
import io.jgitkins.runner.application.port.out.JobFetchPort;
import io.jgitkins.runner.application.port.out.JobResultReportPort;
import io.jgitkins.runner.application.port.out.JobRunnerPort;
import io.jgitkins.runner.application.port.out.RepositorySyncPort;
import io.jgitkins.runner.application.dto.JobRunContext;
import io.jgitkins.runner.domain.RunnerConfiguration;
import io.jgitkins.server.grpc.JobPayload;
import io.jgitkins.server.grpc.JobResultStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class RunnerJobService implements RunnerJobUseCase {

    private final JobFetchPort jobFetchPort;
    private final RepositorySyncPort repositorySyncPort;
    private final JobResultReportPort jobResultReportPort;
    private final JobRunnerPort jobRunnerPort;

    private final RunnerInitService initService;
    private final AtomicBoolean jobInProgress = new AtomicBoolean(false);

    @Override
    public void execute() {
        RunnerConfiguration configuration = initService.getCurrentConfiguration();
        if (configuration == null || !initService.isReadyForScheduling()) {
            return;
        }

        if (jobInProgress.get()) {
            log.debug("Job already running. Skipping poll.");
            return;
        }

        Optional<JobPayload> payloadOptional = jobFetchPort.fetchJob(configuration);
        log.debug("fetching job...");

        payloadOptional.ifPresent(payload -> executeJob(configuration, payload));
    }

    private void executeJob(RunnerConfiguration configuration, JobPayload payload) {
        jobInProgress.set(true);

        try {
            log.info("Starting job {} for repository {}", payload.getJobId(), payload.getRepositoryId());
            Path workspace = repositorySyncPort.fetchRepository(payload.getCloneUrl(),
                                                               Long.toString(payload.getJobId()),
                                                               Long.toString(payload.getRepositoryId()),
                                                               payload.getBranchName());

            JobRunContext context = JobRunContext.builder()
                    .workspacePath(workspace.toAbsolutePath().toString())
                    .runnerImageName(configuration.getRunnerImageName())
                    .pluginPath(null) // TODO: plugin path
                    .build();
            int exitCode = jobRunnerPort.run(context);

            JobResultStatus status = JobResultStatusMapper.fromExitCode(exitCode);

            log.info("jobStatus: [{}]", status);
            jobResultReportPort.reportResult(configuration, payload.getJobId(), status);

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.error("Job {} interrupted", payload.getJobId(), ex);
            jobResultReportPort.reportResult(configuration, payload.getJobId(), JobResultStatus.JOB_RESULT_FAILED);
        } catch (Exception ex) {
            log.error("Job {} execution failed due to unexpected error", payload.getJobId(), ex);
            jobResultReportPort.reportResult(configuration, payload.getJobId(), JobResultStatus.JOB_RESULT_FAILED);
        } finally {
            jobInProgress.set(false);
        }
    }
}
