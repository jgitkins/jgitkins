package io.jgitkins.server.execution.infrastructure.adapter.acl;

import static org.assertj.core.api.Assertions.assertThat;

import io.jgitkins.server.execution.application.port.out.CloneUrlPort;
import io.jgitkins.server.execution.application.service.JobDispatchService;
import io.jgitkins.server.repository.application.support.CloneUrlBuilder;
import java.lang.reflect.Constructor;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;

class ExecutionAclWiringTest {
    @Test void aclAdapter_isTheSoleCloneUrlPortImplementation() {
        assertThat(RepositoryCloneUrlAclAdapter.class.getAnnotation(Component.class)).isNotNull();
        assertThat(RepositoryCloneUrlAclAdapter.class.getInterfaces()).containsExactly(CloneUrlPort.class);
        assertThat(RepositoryCloneUrlAclAdapter.class.getDeclaredFields()).anyMatch(field -> field.getType() == CloneUrlBuilder.class);
        assertThat(JobDispatchService.class.getDeclaredFields()).anyMatch(field -> field.getType() == CloneUrlPort.class);
        assertThat(JobDispatchService.class.getDeclaredFields()).noneMatch(field -> field.getType() == CloneUrlBuilder.class);
        assertThat(Constructor.class).isNotNull();
    }
}
