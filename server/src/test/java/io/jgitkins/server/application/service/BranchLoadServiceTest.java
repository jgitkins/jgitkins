package io.jgitkins.server.repository.application.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import io.jgitkins.server.repository.application.contract.result.BranchSearchResult;
import io.jgitkins.server.repository.application.port.out.BranchQueryPort;
import io.jgitkins.core.common.exception.JgitkinsException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BranchLoadServiceTest {

    @Mock
    private BranchQueryPort branchQueryPort;

    @InjectMocks
    private BranchLoadService service;

    @Test
    void loadBranch_throwsWhenBranchMissing() {
        when(branchQueryPort.findByRepositoryIdAndName(1L, "missing")).thenReturn(Optional.empty());

        assertThrows(JgitkinsException.class, () -> service.loadBranch(1L, "missing"));
    }

    @Test
    void loadBranches_returnsQueryResults() {
        when(branchQueryPort.findAllByRepositoryId(1L)).thenReturn(
                java.util.List.of(new BranchSearchResult(1L, "main", false, false, true))
        );

        java.util.List<BranchSearchResult> results = service.loadBranches(1L);

        org.junit.jupiter.api.Assertions.assertEquals(1, results.size());
        org.junit.jupiter.api.Assertions.assertEquals("main", results.get(0).name());
    }
}
