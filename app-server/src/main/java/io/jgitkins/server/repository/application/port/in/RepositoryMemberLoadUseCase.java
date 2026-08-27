package io.jgitkins.server.repository.application.port.in;

import io.jgitkins.server.repository.application.contract.result.RepositoryMemberSummary;
import java.util.List;

public interface RepositoryMemberLoadUseCase {
    /**
     * @param requesterUserId the authenticated caller. Unlike the repository reads, this one is not
     *     nullable: a member list is never public, so an absent caller is a rejection rather than a
     *     narrower result.
     */
    List<RepositoryMemberSummary> getRepositoryMembers(Long requesterUserId, Long repositoryId);
}
