package io.jgitkins.server.collaboration.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import io.jgitkins.server.collaboration.application.port.out.OrganizeMembershipQueryPort;
import io.jgitkins.server.collaboration.application.validate.OrganizeMemberValidator;
import io.jgitkins.server.collaboration.application.validate.OrganizeValidator;
import io.jgitkins.server.collaboration.domain.repository.OrganizeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class CollaborationApplicationConfigurationTest {
    @Test
    void exposesValidatorBeansThroughExplicitConfiguration() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(OrganizeRepository.class, () -> mock(OrganizeRepository.class));
            context.registerBean(OrganizeMembershipQueryPort.class, () -> mock(OrganizeMembershipQueryPort.class));
            context.register(CollaborationApplicationConfiguration.class);
            context.refresh();

            assertNotNull(context.getBean(OrganizeValidator.class));
            assertNotNull(context.getBean(OrganizeMemberValidator.class));
        }
    }
}
