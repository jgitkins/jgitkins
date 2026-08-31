package io.jgitkins.server.repository.adapter.in.web;

import io.jgitkins.server.shared.application.security.AuthenticatedUser;
import io.jgitkins.server.shared.application.security.CurrentUser;
import io.jgitkins.core.web.api.response.ApiResponse;
import io.jgitkins.server.repository.application.contract.result.RepositoryOverviewResult;
import io.jgitkins.server.repository.application.contract.result.RepositoryResult;
import io.jgitkins.server.repository.application.port.in.RepositoryLoadUseCase;
import io.jgitkins.server.repository.application.port.in.RepositoryOverviewUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Web Repository")
@RequestMapping("/api/internal/repositories")
@Validated
public class WebRepositoryController {

	private final RepositoryLoadUseCase repositoryLoadUseCase;
	private final RepositoryOverviewUseCase repositoryOverviewUseCase;


	/** Anonymous is allowed for these reads; a malformed principal still throws. */
	private static Long optionalRequester(AuthenticatedUser currentUser) {
		return AuthenticatedUser.userIdOrNull(currentUser);
	}


	@Operation(summary = "Get User Repositories by Username (Web)")
	@GetMapping("/users/{username}")
	public ResponseEntity<ApiResponse<List<RepositoryResult>>> getUserRepositories(
			@PathVariable("username") @NotBlank String username,
			@CurrentUser AuthenticatedUser currentUser
	) {
		return ApiResponse.ok(
				repositoryLoadUseCase.loadUserRepositories(optionalRequester(currentUser), username));
	}

	@Operation(summary = "Get Repository Overview by Namespace/Repo (Web)")
	@GetMapping("/{namespace}/{repoName}/overview")
	public ResponseEntity<ApiResponse<RepositoryOverviewResult>> getRepositoryOverviewByPath(
			@PathVariable @NotBlank String namespace,
			@PathVariable @NotBlank String repoName,
			@RequestParam(name = "branch", required = false) String branch,
			@CurrentUser AuthenticatedUser currentUser
	) {
		return ApiResponse.ok(repositoryOverviewUseCase.getOverviewByPath(
				optionalRequester(currentUser), namespace, repoName, branch));
	}
}
