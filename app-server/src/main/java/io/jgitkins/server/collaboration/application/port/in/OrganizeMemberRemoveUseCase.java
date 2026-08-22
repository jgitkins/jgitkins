package io.jgitkins.server.collaboration.application.port.in;

public interface OrganizeMemberRemoveUseCase {
    void removeOrganizeMember(Long organizeId, Long requesterUserId, Long targetUserId);
}
