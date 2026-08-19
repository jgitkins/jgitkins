package io.jgitkins.server.change.review.infrastructure.adapter.acl;

import static org.junit.jupiter.api.Assertions.*;

import io.jgitkins.server.change.review.application.port.out.BranchHeadPort;
import io.jgitkins.server.change.review.application.port.out.RepositoryReferencePort;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;

class ChangeReviewAclWiringTest {
    @Test
    void aclAdaptersAreComponentsOfTheirOwnedPorts() {
        assertTrue(BranchHeadPort.class.isAssignableFrom(BranchHeadAclAdapter.class));
        assertTrue(RepositoryReferencePort.class.isAssignableFrom(RepositoryReferenceAclAdapter.class));
        assertNotNull(BranchHeadAclAdapter.class.getAnnotation(Component.class));
        assertNotNull(RepositoryReferenceAclAdapter.class.getAnnotation(Component.class));
    }

    @Test
    void adaptersExposeOnlyExpectedConstructorDependencies() {
        assertEquals(1, BranchHeadAclAdapter.class.getDeclaredConstructors()[0].getParameterCount());
        assertEquals(3, RepositoryReferenceAclAdapter.class.getDeclaredConstructors()[0].getParameterCount());
    }
}
