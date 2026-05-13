package io.jgitkins.server.application.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import io.jgitkins.server.application.dto.command.JobCreateCommand;
import io.jgitkins.server.execution.domain.repository.JobRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock
    private JobRepository jobPort;

    @InjectMocks
    private JobService service;

    @Test
    void create_savesJobWhenCommandIsProvided() {
        JobCreateCommand command = command(".jgitkins/pipelines/main.Jenkinsfile");

        service.create(command);

        verify(jobPort).save(any());
    }

    private JobCreateCommand command(String pipelineFilePath) {
        return new JobCreateCommand("repo", 9L, "abc1234", "main", pipelineFilePath, 1L);
    }
}
