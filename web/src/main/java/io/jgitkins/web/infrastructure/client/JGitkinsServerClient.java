package io.jgitkins.web.infrastructure.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jgitkins.web.application.dto.*;
import io.jgitkins.web.presentation.common.ApiError;
import io.jgitkins.web.presentation.common.ApiResponse;
import java.io.ByteArrayInputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

@Component
@RequiredArgsConstructor
public class JGitkinsServerClient {

	private static final String MESSAGE_EMPTY_RESPONSE = "API 응답이 비어 있습니다.";
	private static final String MESSAGE_REQUEST_FAILED = "API 요청에 실패했습니다.";
	private static final String MESSAGE_SERVER_UNREACHABLE = "API 서버에 연결할 수 없습니다.";

	private static final ParameterizedTypeReference<ApiResponse<List<OrganizeSummary>>> ORGANIZE_LIST_TYPE =
			new ParameterizedTypeReference<>() {
			};
	private static final ParameterizedTypeReference<ApiResponse<OrganizeSummary>> ORGANIZE_CREATE_TYPE =
			new ParameterizedTypeReference<>() {
			};
	private static final ParameterizedTypeReference<ApiResponse<List<OrganizeMemberSummary>>> ORGANIZE_MEMBER_TYPE =
			new ParameterizedTypeReference<>() {
			};
	private static final ParameterizedTypeReference<ApiResponse<List<RepositorySummary>>> REPOSITORY_LIST_TYPE =
			new ParameterizedTypeReference<>() {
			};
	private static final ParameterizedTypeReference<ApiResponse<RepositorySummary>> REPOSITORY_TYPE =
			new ParameterizedTypeReference<>() {
			};
	private static final ParameterizedTypeReference<ApiResponse<List<CommitSummary>>> COMMIT_LIST_TYPE =
			new ParameterizedTypeReference<>() {
			};
	private static final ParameterizedTypeReference<ApiResponse<List<BranchSummary>>> BRANCH_LIST_TYPE =
			new ParameterizedTypeReference<>() {
			};
	private static final ParameterizedTypeReference<ApiResponse<List<RepositoryFileEntry>>> FILE_LIST_TYPE =
			new ParameterizedTypeReference<>() {
			};
	private static final ParameterizedTypeReference<ApiResponse<List<RepositoryFileIndexEntry>>> FILE_INDEX_LIST_TYPE =
			new ParameterizedTypeReference<>() {
			};
	private static final ParameterizedTypeReference<ApiResponse<RepositoryOverviewResult>> OVERVIEW_TYPE =
			new ParameterizedTypeReference<>() {
			};
	private static final ParameterizedTypeReference<ApiResponse<ServerOAuthLoginResult>> OAUTH_LOGIN_TYPE =
			new ParameterizedTypeReference<>() {
			};
	private static final ParameterizedTypeReference<ApiResponse<RepositorySummary>> REPOSITORY_CREATE_TYPE =
			new ParameterizedTypeReference<>() {
			};
	private static final ParameterizedTypeReference<ApiResponse<List<UserCredentialSummary>>> PAT_LIST_TYPE =
			new ParameterizedTypeReference<>() {
			};
	private static final ParameterizedTypeReference<ApiResponse<UserCredentialIssueResult>> PAT_ISSUE_TYPE =
			new ParameterizedTypeReference<>() {
			};
	private static final ParameterizedTypeReference<ApiResponse<List<UserSummary>>> USER_LIST_TYPE =
			new ParameterizedTypeReference<>() {
			};

	private final RestClient restClient;
	private final ObjectMapper objectMapper;

	public OrganizeFetchResult fetchOrganizes() {
		try {
			ApiResponse<List<OrganizeSummary>> response = restClient.get()
					.uri("/api/organizes")
					.retrieve()
					.body(ORGANIZE_LIST_TYPE);
			if (response == null) {
				return new OrganizeFetchResult(List.of(), MESSAGE_EMPTY_RESPONSE);
			}
			if (response.error() != null) {
				return new OrganizeFetchResult(List.of(), resolveApiErrorMessage(response));
			}
			return new OrganizeFetchResult(response.data() == null ? List.of() : response.data(), null);
		} catch (RestClientException ex) {
			return new OrganizeFetchResult(List.of(), MESSAGE_SERVER_UNREACHABLE);
		}
	}

	public OrganizeFetchResult fetchAccessibleOrganizes() {
		try {
			ApiResponse<List<OrganizeSummary>> response = restClient.get()
					.uri("/api/internal/organizes")
					.retrieve()
					.body(ORGANIZE_LIST_TYPE);
			if (response == null) {
				return new OrganizeFetchResult(List.of(), MESSAGE_EMPTY_RESPONSE);
			}
			if (response.error() != null) {
				return new OrganizeFetchResult(List.of(), resolveApiErrorMessage(response));
			}
			return new OrganizeFetchResult(response.data() == null ? List.of() : response.data(), null);
		} catch (RestClientException ex) {
			return new OrganizeFetchResult(List.of(), MESSAGE_SERVER_UNREACHABLE);
		}
	}

	public List<OrganizeMemberSummary> fetchOrganizeMembers(Long organizeId) {
		try {
			ApiResponse<List<OrganizeMemberSummary>> response = restClient.get()
					.uri("/api/organizes/{organizeId}/members", organizeId)
					.retrieve()
					.body(ORGANIZE_MEMBER_TYPE);
			return extractList(response);
		} catch (RestClientException ex) {
			return List.of();
		}
	}

	public OrganizeCreateResult createOrganize(OrganizeCreateRequest request) {
		try {
			ApiResponse<OrganizeSummary> response = restClient.post()
					.uri("/api/organizes")
					.body(request)
					.retrieve()
					.body(ORGANIZE_CREATE_TYPE);
			if (response == null) {
				return new OrganizeCreateResult(null, MESSAGE_EMPTY_RESPONSE);
			}
			if (response.error() != null) {
				return new OrganizeCreateResult(null, resolveApiErrorMessage(response));
			}
			if (response.data() == null) {
				return new OrganizeCreateResult(null, "조직 생성 응답이 비어 있습니다.");
			}
			return new OrganizeCreateResult(response.data(), null);
		} catch (RestClientResponseException ex) {
			return new OrganizeCreateResult(null, resolveErrorMessage(ex.getResponseBodyAsString(), MESSAGE_REQUEST_FAILED));
		} catch (RestClientException ex) {
			return new OrganizeCreateResult(null, MESSAGE_SERVER_UNREACHABLE);
		}
	}

	public List<RepositorySummary> fetchRepositories() {
		try {
			ApiResponse<List<RepositorySummary>> response = restClient.get()
					.uri("/api/repositories")
					.retrieve()
					.body(REPOSITORY_LIST_TYPE);
			return extractList(response);
		} catch (RestClientException ex) {
			return List.of();
		}
	}

	public List<RepositorySummary> fetchRepositoriesByUsername(String username) {
		try {
			ApiResponse<List<RepositorySummary>> response = restClient.get()
					.uri("/api/internal/repositories/users/{username}", username)
					.retrieve()
					.body(REPOSITORY_LIST_TYPE);
			return extractList(response);
		} catch (RestClientException ex) {
			return List.of();
		}
	}

	public RepositorySummary fetchRepository(Long repositoryId) {
		try {
			ApiResponse<RepositorySummary> response = restClient.get()
					.uri("/api/repositories/{repositoryId}", repositoryId)
					.retrieve()
					.body(REPOSITORY_TYPE);
			return extractData(response);
		} catch (RestClientException ex) {
			return null;
		}
	}

	public RepositoryOverviewResult fetchRepositoryOverview(Long repositoryId, String branch) {
		try {
			ApiResponse<RepositoryOverviewResult> response = restClient.get()
					.uri(uriBuilder -> uriBuilder
							.path("/api/repositories/{repositoryId}/overview")
							.queryParamIfPresent("branch", branch == null || branch.isBlank()
									? java.util.Optional.empty()
									: java.util.Optional.of(branch))
							.build(repositoryId))
					.retrieve()
					.body(OVERVIEW_TYPE);
			return extractData(response);
		} catch (RestClientException ex) {
			return null;
		}
	}

	public RepositoryOverviewResult fetchRepositoryOverviewByPath(String namespace, String repoName, String branch) {
		try {
			ApiResponse<RepositoryOverviewResult> response = restClient.get()
					.uri(uriBuilder -> uriBuilder
							.path("/api/internal/repositories/{namespace}/{repoName}/overview")
							.queryParamIfPresent("branch", branch == null || branch.isBlank()
									? java.util.Optional.empty()
									: java.util.Optional.of(branch))
							.build(namespace, repoName))
					.retrieve()
					.body(OVERVIEW_TYPE);
			return extractData(response);
		} catch (RestClientException ex) {
			return null;
		}
	}

	public List<CommitSummary> fetchCommits(String namespace, String repoName, String branch) {
		try {
			ApiResponse<List<CommitSummary>> response = restClient.get()
					.uri("/repositories/{namespace}/{repo}/branches/{branch}/commits", namespace, repoName, branch)
					.retrieve()
					.body(COMMIT_LIST_TYPE);
			return extractList(response);
		} catch (RestClientException ex) {
			return List.of();
		}
	}

	public List<BranchSummary> fetchBranches(Long repositoryId) {
		try {
			ApiResponse<List<BranchSummary>> response = restClient.get()
					.uri("/api/repositories/{repositoryId}/branches", repositoryId)
					.retrieve()
					.body(BRANCH_LIST_TYPE);
			return extractList(response);
		} catch (RestClientException ex) {
			return List.of();
		}
	}

	public List<RepositoryFileEntry> fetchRepositoryFiles(String namespace, String repoName, String branch) {
		try {
			ApiResponse<List<RepositoryFileEntry>> response = restClient.get()
					.uri("/repositories/{namespace}/{repo}/files?ref={branch}", namespace, repoName, branch)
					.retrieve()
					.body(FILE_LIST_TYPE);
			return extractList(response);
		} catch (RestClientException ex) {
			return List.of();
		}
	}

	public List<RepositoryFileIndexEntry> fetchRepositoryFileIndex(String namespace, String repoName, String branch) {
		try {
			ApiResponse<List<RepositoryFileIndexEntry>> response = restClient.get()
					.uri("/repositories/{namespace}/{repo}/files/index?ref={branch}", namespace, repoName, branch)
					.retrieve()
					.body(FILE_INDEX_LIST_TYPE);
			return extractList(response);
		} catch (RestClientException ex) {
			return List.of();
		}
	}

	public List<RepositoryFileEntry> fetchRepositoryTree(String namespace, String repoName, String branch, String directory) {
		if (branch == null || branch.isBlank()) {
			return List.of();
		}
		try {
			ApiResponse<List<RepositoryFileEntry>> response = restClient.get()
					.uri(uriBuilder -> uriBuilder
							.path("/api/repositories/{namespace}/{repo}/refs/{branch}/tree")
							.queryParamIfPresent("dir", directory == null || directory.isBlank()
									? java.util.Optional.empty()
									: java.util.Optional.of(directory))
							.build(namespace, repoName, branch))
					.retrieve()
					.body(FILE_LIST_TYPE);
			return extractList(response);
		} catch (RestClientException ex) {
			return List.of();
		}
	}

	public ServerOAuthLoginResult issueOAuthLoginToken(OAuthLoginRequest request) {
		ApiResponse<ServerOAuthLoginResult> response = restClient.post()
				.uri("/api/auth/oauth/login")
				.body(request)
				.retrieve()
				.body(OAUTH_LOGIN_TYPE);
		if (response == null || response.error() != null || response.data() == null) {
			throw new RestClientException("OAuth login failed");
		}
		return response.data();
	}

	public RepositoryCreateResult createRepository(RepositoryCreateRequest request) {
		try {
			ApiResponse<RepositorySummary> response = restClient.post()
					.uri("/api/repositories")
					.body(request)
					.retrieve()
					.body(REPOSITORY_CREATE_TYPE);
			if (response == null) {
				return new RepositoryCreateResult(null, MESSAGE_EMPTY_RESPONSE);
			}
			if (response.error() != null) {
				return new RepositoryCreateResult(null, resolveApiErrorMessage(response));
			}
			if (response.data() == null) {
				return new RepositoryCreateResult(null, "저장소 생성 응답이 비어 있습니다.");
			}
			return new RepositoryCreateResult(response.data(), null);
		} catch (RestClientResponseException ex) {
			return new RepositoryCreateResult(null, resolveErrorMessage(ex.getResponseBodyAsString(), MESSAGE_REQUEST_FAILED));
		} catch (RestClientException ex) {
			return new RepositoryCreateResult(null, MESSAGE_SERVER_UNREACHABLE);
		}
	}

	public RepositoryBranchCreateResult createBranch(Long repositoryId, String branchName, String sourceBranch) {
		try {
			restClient.post()
					.uri("/api/repositories/{repositoryId}/branches", repositoryId)
					.body(new BranchCreatePayload(branchName, sourceBranch))
					.retrieve()
					.toBodilessEntity();
			BranchSummary createdBranch = new BranchSummary(repositoryId, branchName, false, false, false);
			return new RepositoryBranchCreateResult(createdBranch, null);
		} catch (RestClientResponseException ex) {
			return new RepositoryBranchCreateResult(null, resolveErrorMessage(ex.getResponseBodyAsString(), MESSAGE_REQUEST_FAILED));
		} catch (RestClientException ex) {
			return new RepositoryBranchCreateResult(null, MESSAGE_SERVER_UNREACHABLE);
		}
	}

	public RepositoryFileUploadResult uploadFile(RepositoryFileUploadRequest request) {
		try {
			MultipartBodyBuilder builder = new MultipartBodyBuilder();
			builder.part("branch", request.branch());
			builder.part("path", request.path());
			builder.part("message", request.message());
			builder.part("file", buildFileResource(request.content(), request.originalFilename()))
					.contentType(resolveFileMediaType(request.contentType()));

			restClient.post()
					.uri("/api/repositories/{repositoryId}/files", request.repositoryId())
					.contentType(MediaType.MULTIPART_FORM_DATA)
					.body(builder.build())
					.retrieve()
					.toBodilessEntity();
			return new RepositoryFileUploadResult(null);
		} catch (RestClientResponseException ex) {
			return new RepositoryFileUploadResult(resolveErrorMessage(ex.getResponseBodyAsString(), MESSAGE_REQUEST_FAILED));
		} catch (RestClientException ex) {
			return new RepositoryFileUploadResult(MESSAGE_SERVER_UNREACHABLE);
		}
	}

	public List<UserCredentialSummary> fetchPersonalAccessTokens() {
		try {
			ApiResponse<List<UserCredentialSummary>> response = restClient.get()
					.uri("/api/auth/pats")
					.retrieve()
					.body(PAT_LIST_TYPE);
			return extractList(response);
		} catch (RestClientException ex) {
			return List.of();
		}
	}

	public List<UserSummary> fetchUsers() {
		try {
			ApiResponse<List<UserSummary>> response = restClient.get()
					.uri("/api/users")
					.retrieve()
					.body(USER_LIST_TYPE);
			return extractList(response);
		} catch (RestClientException ex) {
			return List.of();
		}
	}

	public UsernameUpdateResult updateUsername(UsernameUpdateRequest request) {
		try {
			ApiResponse<Void> response = restClient.patch()
					.uri("/api/users/me/username")
					.body(request)
					.retrieve()
					.body(new ParameterizedTypeReference<ApiResponse<Void>>() {
					});
			if (response == null) {
				return new UsernameUpdateResult(MESSAGE_EMPTY_RESPONSE);
			}
			if (response.error() != null) {
				return new UsernameUpdateResult(resolveApiErrorMessage(response));
			}
			return new UsernameUpdateResult(null);
		} catch (RestClientResponseException ex) {
			return new UsernameUpdateResult(resolveErrorMessage(ex.getResponseBodyAsString(), MESSAGE_REQUEST_FAILED));
		} catch (RestClientException ex) {
			return new UsernameUpdateResult(MESSAGE_SERVER_UNREACHABLE);
		}
	}

	public UserCredentialIssueResult issuePersonalAccessToken(UserCredentialIssueRequest request) {
		ApiResponse<UserCredentialIssueResult> response = restClient.post()
				.uri("/api/auth/pats")
				.body(request)
				.retrieve()
				.body(PAT_ISSUE_TYPE);
		if (response == null || response.error() != null || response.data() == null) {
			throw new RestClientException("Personal access token issue failed");
		}
		return response.data();
	}

	public void revokePersonalAccessToken(Long credentialId) {
		try {
			restClient.delete()
					.uri("/api/auth/pats/{credentialId}", credentialId)
					.retrieve()
					.body(Void.class);
		} catch (RestClientException ex) {
			throw new RestClientException("Personal access token revoke failed");
		}
	}

	private <T> T extractData(ApiResponse<T> response) {
		if (response == null || response.error() != null) {
			return null;
		}
		return response.data();
	}

	private <T> List<T> extractList(ApiResponse<List<T>> response) {
		if (response == null || response.error() != null || response.data() == null) {
			return List.of();
		}
		return response.data();
	}

	private String resolveApiErrorMessage(ApiResponse<?> response) {
		if (response == null || response.error() == null) {
			return MESSAGE_REQUEST_FAILED;
		}
		ApiError error = response.error();
		if (!StringUtils.hasText(error.message())) {
			return MESSAGE_REQUEST_FAILED;
		}
		return error.message();
	}

	private String resolveErrorMessage(String responseBody, String fallbackMessage) {
		if (!StringUtils.hasText(responseBody)) {
			return fallbackMessage;
		}
		try {
			ApiResponse<Object> response = objectMapper.readValue(responseBody, new TypeReference<>() {
			});
			if (response != null && response.error() != null && StringUtils.hasText(response.error().message())) {
				return response.error().message();
			}
		} catch (JsonProcessingException ignored) {
			// no-op
		}
		return fallbackMessage;
	}

	private InputStreamResource buildFileResource(byte[] content, String filename) {
		byte[] safeContent = content == null ? new byte[0] : content;
		return new InputStreamResource(new ByteArrayInputStream(safeContent)) {
			@Override
			public String getFilename() {
				return filename;
			}

			@Override
			public long contentLength() {
				return safeContent.length;
			}
		};
	}

	private MediaType resolveFileMediaType(String contentType) {
		if (!StringUtils.hasText(contentType)) {
			return MediaType.APPLICATION_OCTET_STREAM;
		}
		try {
			return MediaType.parseMediaType(contentType);
		} catch (IllegalArgumentException ex) {
			return MediaType.APPLICATION_OCTET_STREAM;
		}
	}

	private record BranchCreatePayload(String branchName, String sourceBranch) {
	}
}
