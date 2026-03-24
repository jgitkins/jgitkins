package io.jgitkins.server.application.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import io.jgitkins.server.application.dto.command.JobCreateCommand;
import io.jgitkins.server.application.port.out.JobPersistencePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock
    private JobPersistencePort jobPort;

    @InjectMocks
    private JobService service;

    @Test
    void create_savesJobWhenCommandIsProvided() {
        JobCreateCommand command = command(".jgitkins/pipelines/main.Jenkinsfile");

        service.create(command);

        verify(jobPort).save(any());
    }

    private JobCreateCommand command(String pipelineFilePath) {
        return JobCreateCommand.builder()
                .repoName("repo")
                .repositoryId(9L)
                .commitHash("abc1234")
                .branchName("main")
                .pipelineFilePath(pipelineFilePath)
                .triggeredBy(1L)
                .build();
    }
}
