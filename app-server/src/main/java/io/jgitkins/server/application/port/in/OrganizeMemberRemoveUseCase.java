package io.jgitkins.server.application.port.in;

public interface OrganizeMemberRemoveUseCase {
    void removeOrganizeMember(Long organizeId, Long userId);
}
