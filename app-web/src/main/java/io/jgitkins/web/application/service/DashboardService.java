package io.jgitkins.web.application.service;

import io.jgitkins.web.application.contract.CommitSummary;
import io.jgitkins.web.application.contract.DashboardData;
import io.jgitkins.web.application.contract.OrganizeFetchResult;
import io.jgitkins.web.application.contract.OrganizeSummary;
import io.jgitkins.web.application.contract.RepositoryCommits;
import io.jgitkins.web.application.contract.RepositorySummary;
import io.jgitkins.web.application.model.RepositoryKey;
import io.jgitkins.web.application.port.in.DashboardUseCase;
import io.jgitkins.web.application.port.out.OrganizePort;
import io.jgitkins.web.application.port.out.RepositoryPort;
import io.jgitkins.web.infrastructure.util.PathUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService implements DashboardUseCase {

	private static final int MAX_DASHBOARD_REPOSITORIES = 10;

	private final OrganizePort organizePort;
	private final RepositoryPort repositoryPort;

	@Override
	public DashboardData buildDashboardForUser(String username) {
		List<RepositorySummary> repositories = (username == null || username.isBlank())
				? repositoryPort.fetchRepositories()
				: repositoryPort.fetchRepositoriesByUsername(username);

		OrganizeFetchResult organizeResult = organizePort.fetchAccessibleOrganizes();
		List<OrganizeSummary> organizes = organizeResult.organizes();

		List<RepositoryCommits> items = buildRepositoryCommits(repositories);
		return new DashboardData(
				organizes,
				items,
				organizeResult.errorMessage()
		);
	}


	private List<RepositoryCommits> buildRepositoryCommits(List<RepositorySummary> repositories) {
		List<RepositoryCommits> items = new ArrayList<>();
		for (RepositorySummary repository : repositories) {
			if (items.size() >= MAX_DASHBOARD_REPOSITORIES) {
				break;
			}
			RepositoryKey key = resolveRepositoryKey(repository);
			if (key == null) {
				continue;
			}
			String branch = StringUtils.hasText(repository.defaultBranch()) ? repository.defaultBranch() : "main";
			List<CommitSummary> commits = repositoryPort.fetchCommits(key.namespace(), key.repoName(), branch).stream()
					.limit(10)
					.toList();
			items.add(new RepositoryCommits(
					key.namespace(),
					key.repoName(),
					repository,
					commits
			));
		}
		return items;
	}

	private RepositoryKey resolveRepositoryKey(RepositorySummary repository) {
		if (repository == null) {
			return null;
		}
		RepositoryKey key = PathUtils.resolveRepositoryKey(repository.clonePath(), repository.path());
		if (key != null) {
			return key;
		}
		if (StringUtils.hasText(repository.name())) {
			return new RepositoryKey("unknown", repository.name());
		}
		return null;
	}
}
