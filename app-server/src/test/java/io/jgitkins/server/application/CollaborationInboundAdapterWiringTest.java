package io.jgitkins.server.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.jgitkins.server.collaboration.adapter.in.rest.translator.OrganizeMemberRequestMapper;
import io.jgitkins.server.collaboration.adapter.in.rest.translator.OrganizeRequestMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest
class CollaborationInboundAdapterWiringTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void requestMappers_areSpringBeans() {
        assertThat(applicationContext.getBean(OrganizeRequestMapper.class)).isNotNull();
        assertThat(applicationContext.getBean(OrganizeMemberRequestMapper.class)).isNotNull();
    }
}
