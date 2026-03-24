package io.jgitkins.server.application.validate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import io.jgitkins.server.common.exception.JgitkinsException;
import io.jgitkins.server.application.dto.command.BranchCreateCommand;
import io.jgitkins.server.application.port.out.BranchPersistencePort;
import io.jgitkins.server.domain.Branch;
import io.jgitkins.server.domain.aggregate.Repository;
import io.jgitkins.server.domain.model.vo.BranchName;
import io.jgitkins.server.domain.model.vo.RepositoryId;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BranchCreationValidatorTest {

    @Mock
    private BranchPersistencePort branchPort;

    @InjectMocks
    private BranchCreationValidator validator;

    @Test
    void validateBranchDoesNotExist_throwsWhenBranchAlreadyExists() {
        when(branchPort.findByRepositoryIdAndName(1L, "feature")).thenReturn(Optional.of(Branch.create(1L, "feature")));

        assertThrows(JgitkinsException.class, () -> validator.validateBranchDoesNotExist(1L, "feature"));
    }

    @Test
    void validateRepositoryInitialized_throwsWhenRepositoryNotInitialized() {
        Repository repository = org.mockito.Mockito.mock(Repository.class);
        when(repository.isInitialized()).thenReturn(false);

        assertThrows(JgitkinsException.class, () -> validator.validateRepositoryInitialized(repository));
    }

    @Test
    void resolveAndValidateSourceBranch_usesDefaultBranchWhenSourceMissing() {
        Repository repository = org.mockito.Mockito.mock(Repository.class);
        when(repository.getId()).thenReturn(RepositoryId.of(1L));
        when(repository.getDefaultBranch()).thenReturn(BranchName.of("main"));
        when(branchPort.findByRepositoryIdAndName(1L, "main")).thenReturn(Optional.of(Branch.create(1L, "main")));

        BranchCreateCommand command = BranchCreateCommand.builder()
                .repositoryId(1L)
                .branchName("feature")
                .sourceBranch(null)
                .build();

        String sourceBranch = validator.resolveAndValidateSourceBranch(command, repository);

        assertEquals("main", sourceBranch);
    }

    @Test
    void resolveAndValidateSourceBranch_throwsWhenSourceBranchMissing() {
        Repository repository = org.mockito.Mockito.mock(Repository.class);
        when(repository.getId()).thenReturn(RepositoryId.of(1L));
        when(branchPort.findByRepositoryIdAndName(1L, "dev")).thenReturn(Optional.empty());

        BranchCreateCommand command = BranchCreateCommand.builder()
                .repositoryId(1L)
                .branchName("feature")
                .sourceBranch("dev")
                .build();

        assertThrows(JgitkinsException.class, () -> validator.resolveAndValidateSourceBranch(command, repository));
    }

    @Test
    void validateNotDefaultBranch_throwsWhenDeletingDefaultBranch() {
        Repository repository = org.mockito.Mockito.mock(Repository.class);
        Branch defaultBranch = Branch.create(1L, "main", false, false, true);
        when(repository.getDefaultBranch()).thenReturn(BranchName.of("main"));

        assertThrows(JgitkinsException.class, () -> validator.validateNotDefaultBranch(repository, defaultBranch));
    }
}
