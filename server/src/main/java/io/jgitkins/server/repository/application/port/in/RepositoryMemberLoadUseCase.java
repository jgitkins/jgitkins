package io.jgitkins.server.repository.application.port.in;

import io.jgitkins.server.repository.application.contract.result.RepositoryMemberSummary;
import java.util.List;

public interface RepositoryMemberLoadUseCase {
    List<RepositoryMemberSummary> getRepositoryMembers(Long repositoryId);
}
