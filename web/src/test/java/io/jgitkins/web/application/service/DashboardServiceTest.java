package io.jgitkins.web.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jgitkins.web.application.dto.CommitSummary;
import io.jgitkins.web.application.dto.DashboardData;
import io.jgitkins.web.application.dto.OrganizeFetchResult;
import io.jgitkins.web.application.dto.RepositorySummary;
import io.jgitkins.web.application.port.out.OrganizePort;
import io.jgitkins.web.application.port.out.RepositoryPort;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

	@Mock
	private OrganizePort organizePort;
	@Mock
	private RepositoryPort repositoryPort;

	private DashboardService dashboardService;

	@BeforeEach
	void setUp() {
		dashboardService = new DashboardService(organizePort, repositoryPort);
	}

	@Test
	void buildDashboardForUser_limitsCommitLookupsToTenRepositories() {
		List<RepositorySummary> repositories = new ArrayList<>();
		for (int i = 1; i <= 12; i++) {
			repositories.add(repository(i));
		}

		when(repositoryPort.fetchRepositoriesByUsername("alzar")).thenReturn(repositories);
		when(repositoryPort.fetchCommits(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString()))
				.thenReturn(List.of(new CommitSummary("c1", "author", "a@b.c", "msg", LocalDateTime.now())));
		when(organizePort.fetchAccessibleOrganizes()).thenReturn(new OrganizeFetchResult(List.of(), null));

		DashboardData result = dashboardService.buildDashboardForUser("alzar");

		assertEquals(10, result.repositories().size());
		verify(repositoryPort, times(10)).fetchCommits(
				org.mockito.ArgumentMatchers.anyString(),
				org.mockito.ArgumentMatchers.anyString(),
				org.mockito.ArgumentMatchers.anyString()
		);
	}

	private RepositorySummary repository(int index) {
		String name = "repo-" + index;
		return new RepositorySummary(
				(long) index,
				"USER",
				name,
				"alzar/" + name,
				"main",
				"PRIVATE",
				null,
				1L,
				"git@server:alzar/" + name + ".git",
				null,
				null,
				null,
				null
		);
	}
}
