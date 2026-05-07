package io.jgitkins.server.application.support.change;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.jgitkins.server.application.dto.command.PushEventCommand;
import io.jgitkins.server.repository.domain.repository.BranchRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BranchChangeRecorderTest {

    @Mock
    private BranchRepository branchRepository;

    @InjectMocks
    private BranchChangeRecorder branchChangeRecorder;

    @Test
    void record_savesBranchWhenCreated() {
        PushEventCommand command = PushEventCommand.builder()
                .repositoryId(1L)
                .branchName("feature/test")
                .branchCreated(true)
                .build();

        branchChangeRecorder.record(command);

        verify(branchRepository).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void record_deletesBranchWhenDeleted() {
        PushEventCommand command = PushEventCommand.builder()
                .repositoryId(1L)
                .branchName("feature/test")
                .branchDeleted(true)
                .build();

        branchChangeRecorder.record(command);

        verify(branchRepository).delete(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void record_skipsPersistenceWhenRepositoryIdMissing() {
        PushEventCommand command = PushEventCommand.builder()
                .branchName("feature/test")
                .branchCreated(true)
                .build();

        branchChangeRecorder.record(command);

        verify(branchRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(branchRepository, never()).delete(org.mockito.ArgumentMatchers.any());
    }
}
