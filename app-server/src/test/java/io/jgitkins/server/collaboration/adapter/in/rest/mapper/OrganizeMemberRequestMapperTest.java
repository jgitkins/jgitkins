package io.jgitkins.server.collaboration.adapter.in.rest.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import io.jgitkins.server.collaboration.adapter.in.rest.dto.request.OrganizeMemberAddRequest;
import io.jgitkins.server.collaboration.application.contract.command.OrganizeMemberAddCommand;
import io.jgitkins.server.collaboration.domain.vo.OrganizeMemberRole;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class OrganizeMemberRequestMapperTest {

    private final OrganizeMemberRequestMapper mapper = Mappers.getMapper(OrganizeMemberRequestMapper.class);

    @Test
    void toCommand_mapsPathVariableAndRequestFields() {
        OrganizeMemberAddCommand command = mapper.toCommand(
                41L,
                new OrganizeMemberAddRequest(9L, OrganizeMemberRole.MEMBER),
                7L);

        assertThat(command.organizeId()).isEqualTo(41L);
        assertThat(command.userId()).isEqualTo(9L);
        assertThat(command.role()).isEqualTo(OrganizeMemberRole.MEMBER);
        assertThat(command.requesterUserId()).isEqualTo(7L);
    }
}
