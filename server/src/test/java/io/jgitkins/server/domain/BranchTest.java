package io.jgitkins.server.domain;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.jgitkins.server.common.exception.JgitkinsException;
import org.junit.jupiter.api.Test;

class BranchTest {

    @Test
    void delete_throwsWhenBranchIsDefault() {
        Branch branch = Branch.create(1L, "main", false, false, true);

        assertThrows(JgitkinsException.class, branch::delete);
    }

    @Test
    void delete_doesNotThrowWhenBranchIsNotDefault() {
        Branch branch = Branch.create(1L, "feature");

        assertDoesNotThrow(branch::delete);
    }
}
