package io.jgitkins.server.execution.adapter.out.acl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jgitkins.server.repository.application.service.internal.CloneUrlBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RepositoryCloneUrlAclAdapterTest {
    @Mock CloneUrlBuilder builder;
    @Test void build_delegatesToRepositoryAcl() {
        when(builder.build("org/repo.git")).thenReturn("https://host/org/repo.git");
        assertThat(new RepositoryCloneUrlAclAdapter(builder).build("org/repo.git")).isEqualTo("https://host/org/repo.git");
        verify(builder).build("org/repo.git");
    }
}
