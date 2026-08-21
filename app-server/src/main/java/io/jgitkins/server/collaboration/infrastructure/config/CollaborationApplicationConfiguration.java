package io.jgitkins.server.collaboration.infrastructure.config;

import io.jgitkins.server.collaboration.application.port.out.OrganizeMembershipQueryPort;
import io.jgitkins.server.collaboration.application.validate.OrganizeMemberValidator;
import io.jgitkins.server.collaboration.application.validate.OrganizeValidator;
import io.jgitkins.server.collaboration.domain.repository.OrganizeRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CollaborationApplicationConfiguration {
    @Bean
    OrganizeValidator organizeValidator(
            OrganizeRepository organizeRepository,
            OrganizeMembershipQueryPort organizeMembershipQueryPort) {
        return new OrganizeValidator(organizeRepository, organizeMembershipQueryPort);
    }

    @Bean
    OrganizeMemberValidator organizeMemberValidator(
            OrganizeMembershipQueryPort organizeMembershipQueryPort) {
        return new OrganizeMemberValidator(organizeMembershipQueryPort);
    }
}
