package io.jgitkins.server.application.validate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import io.jgitkins.server.common.exception.JgitkinsException;
import io.jgitkins.server.repository.application.contract.command.BranchCreateCommand;
import io.jgitkins.server.domain.Branch;
import io.jgitkins.server.domain.aggregate.Repository;
import io.jgitkins.server.domain.model.vo.BranchName;
import io.jgitkins.server.domain.model.vo.RepositoryId;
import io.jgitkins.server.domain.repository.BranchRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BranchCreationValidatorTest {

    @Mock
    private BranchRepository branchPort;

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

        BranchCreateCommand command = new BranchCreateCommand(1L, "feature", null, false);

        String sourceBranch = validator.resolveAndValidateSourceBranch(command, repository);

        assertEquals("main", sourceBranch);
    }

    @Test
    void resolveAndValidateSourceBranch_throwsWhenSourceBranchMissing() {
        Repository repository = org.mockito.Mockito.mock(Repository.class);
        when(repository.getId()).thenReturn(RepositoryId.of(1L));
        when(branchPort.findByRepositoryIdAndName(1L, "dev")).thenReturn(Optional.empty());

        BranchCreateCommand command = new BranchCreateCommand(1L, "feature", "dev", false);

        assertThrows(JgitkinsException.class, () -> validator.resolveAndValidateSourceBranch(command, repository));
    }
}
