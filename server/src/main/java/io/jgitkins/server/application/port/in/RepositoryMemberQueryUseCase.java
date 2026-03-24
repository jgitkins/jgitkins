package io.jgitkins.server.application.port.in;

import io.jgitkins.server.application.dto.result.RepositoryMemberSummary;
import java.util.List;

public interface RepositoryMemberQueryUseCase {
    List<RepositoryMemberSummary> getRepositoryMembers(Long repositoryId);
}
