package io.jgitkins.web.application.service;

import io.jgitkins.web.application.dto.NamespaceSummary;
import io.jgitkins.web.application.dto.OrganizeFetchResult;
import io.jgitkins.web.application.dto.OrganizeMemberSummary;
import io.jgitkins.web.application.dto.OrganizeSummary;
import io.jgitkins.web.application.dto.RepositorySummary;
import io.jgitkins.web.application.model.RepositoryKey;
import io.jgitkins.web.application.port.in.NamespaceDetailUseCase;
import io.jgitkins.web.application.port.out.OrganizePort;
import io.jgitkins.web.application.port.out.RepositoryPort;
import io.jgitkins.web.infrastructure.util.PathUtils;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class NamespaceDetailService implements NamespaceDetailUseCase {

	private final OrganizePort organizePort;
	private final RepositoryPort repositoryPort;

	@Override
	public NamespaceSummary loadNamespaceDetail(String namespace) {
		if (!StringUtils.hasText(namespace)) {
			return new NamespaceSummary(false, null, null, List.of(), List.of(), "이름이 필요합니다.");
		}

		OrganizeFetchResult organizeResult = organizePort.fetchOrganizes();
		if (organizeResult.errorMessage() != null) {
			return new NamespaceSummary(false, namespace, null, List.of(), List.of(), organizeResult.errorMessage());
		}

		List<RepositorySummary> repositories = repositoryPort.fetchRepositories();
		Optional<OrganizeSummary> organize = organizeResult.organizes().stream()
				.filter(item -> item != null && namespace.equalsIgnoreCase(item.name()))
				.findFirst();
		if (organize.isPresent()) {
			List<RepositorySummary> organizeRepos = repositories.stream()
					.filter(repo -> repo != null)
					.filter(repo -> "ORGANIZATION".equalsIgnoreCase(repo.ownerType()))
					.filter(repo -> repo.ownerId() != null && repo.ownerId().equals(organize.get().id()))
					.collect(Collectors.toList());
			List<OrganizeMemberSummary> members = organizePort.fetchOrganizeMembers(organize.get().id());
			return new NamespaceSummary(
					true,
					organize.get().name(),
					organize.get().description(),
					organizeRepos,
					members,
					null);
		}

		List<RepositorySummary> userRepos = repositories.stream()
				.filter(repo -> repo != null)
				.filter(repo -> namespace.equalsIgnoreCase(resolveNamespaceSlug(resolveNamespace(repo))))
				.collect(Collectors.toList());

		return new NamespaceSummary(
				false,
				namespace,
				"Personal namespace",
				userRepos,
				List.of(),
				null);
	}

	private String resolveNamespace(RepositorySummary repository) {
		if (repository == null) {
			return null;
		}
		RepositoryKey key = PathUtils.resolveRepositoryKey(repository.clonePath(), repository.path());
		return key == null ? null : key.namespace();
	}

	private String resolveNamespaceSlug(String namespace) {
		String segment = PathUtils.lastSegment(namespace);
		return segment.isBlank() ? null : segment;
	}
}
