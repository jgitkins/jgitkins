package io.jgitkins.web.infrastructure.adapter;

import io.jgitkins.web.application.dto.*;
import io.jgitkins.web.application.port.out.AppTokenIssuePort;
import io.jgitkins.web.application.port.out.OrganizePort;
import io.jgitkins.web.application.port.out.RepositoryPort;
import io.jgitkins.web.application.port.out.UserPort;
import io.jgitkins.web.application.port.out.UserCredentialPort;
import io.jgitkins.web.infrastructure.client.JGitkinsServerClient;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JGitkinsServerAdapter implements OrganizePort, RepositoryPort, AppTokenIssuePort, UserCredentialPort, UserPort {

	private final JGitkinsServerClient serverClient;

	@Override
	public OrganizeFetchResult fetchOrganizes() {
		return serverClient.fetchOrganizes();
	}

	@Override
	public OrganizeFetchResult fetchAccessibleOrganizes() {
		return serverClient.fetchAccessibleOrganizes();
	}

	@Override
	public OrganizeCreateResult createOrganize(OrganizeCreateRequest request) {
		return serverClient.createOrganize(request);
	}

	@Override
	public List<OrganizeMemberSummary> fetchOrganizeMembers(Long organizeId) {
		return serverClient.fetchOrganizeMembers(organizeId);
	}

	@Override
	public List<RepositorySummary> fetchRepositories() {
		return serverClient.fetchRepositories();
	}

	@Override
	public List<RepositorySummary> fetchRepositoriesByUsername(String username) {
		return serverClient.fetchRepositoriesByUsername(username);
	}

	@Override
	public RepositorySummary fetchRepository(Long repositoryId) {
		return serverClient.fetchRepository(repositoryId);
	}

	@Override
	public RepositoryOverviewResult fetchRepositoryOverview(Long repositoryId, String branch) {
		return serverClient.fetchRepositoryOverview(repositoryId, branch);
	}

	@Override
	public RepositoryOverviewResult fetchRepositoryOverviewByPath(String namespace, String repoName, String branch) {
		return serverClient.fetchRepositoryOverviewByPath(namespace, repoName, branch);
	}

	@Override
	public List<CommitSummary> fetchCommits(String namespace, String repoName, String branch) {
		return serverClient.fetchCommits(namespace, repoName, branch);
	}

	@Override
	public List<BranchSummary> fetchBranches(Long repositoryId) {
		return serverClient.fetchBranches(repositoryId);
	}

	@Override
	public List<RepositoryFileEntry> fetchRepositoryFiles(String namespace, String repoName, String branch) {
		return serverClient.fetchRepositoryFiles(namespace, repoName, branch);
	}

	@Override
	public List<RepositoryFileIndexEntry> fetchRepositoryFileIndex(String namespace, String repoName, String branch) {
		return serverClient.fetchRepositoryFileIndex(namespace, repoName, branch);
	}

	@Override
	public List<RepositoryFileEntry> fetchRepositoryTree(String namespace, String repoName, String branch, String directory) {
		return serverClient.fetchRepositoryTree(namespace, repoName, branch, directory);
	}

	@Override
	public ServerOAuthLoginResult issueOAuthLoginToken(OAuthLoginRequest request) {
		return serverClient.issueOAuthLoginToken(request);
	}

	@Override
	public RepositoryCreateResult createRepository(RepositoryCreateRequest request) {
		return serverClient.createRepository(request);
	}

	@Override
	public RepositoryBranchCreateResult createBranch(Long repositoryId, String branchName, String sourceBranch) {
		return serverClient.createBranch(repositoryId, branchName, sourceBranch);
	}

	@Override
	public RepositoryFileUploadResult uploadFile(RepositoryFileUploadRequest request) {
		return serverClient.uploadFile(request);
	}

	@Override
	public List<UserCredentialSummary> fetchPersonalAccessTokens() {
		return serverClient.fetchPersonalAccessTokens();
	}

	@Override
	public UserCredentialIssueResult issuePersonalAccessToken(UserCredentialIssueRequest request) {
		return serverClient.issuePersonalAccessToken(request);
	}

	@Override
	public void revokePersonalAccessToken(Long credentialId) {
		serverClient.revokePersonalAccessToken(credentialId);
	}

	@Override
	public List<UserSummary> fetchUsers() {
		return serverClient.fetchUsers();
	}

	@Override
	public UsernameUpdateResult updateUsername(String username) {
		return serverClient.updateUsername(new UsernameUpdateRequest(username));
	}
}
