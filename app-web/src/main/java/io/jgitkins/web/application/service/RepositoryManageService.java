package io.jgitkins.web.application.service;

import io.jgitkins.web.application.dto.RepositoryBranchCreateResult;
import io.jgitkins.web.application.dto.RepositoryFileUploadRequest;
import io.jgitkins.web.application.dto.RepositoryFileUploadResult;
import io.jgitkins.web.application.dto.RepositorySummary;
import io.jgitkins.web.application.model.RepositoryKey;
import io.jgitkins.web.application.port.in.RepositoryManageUseCase;
import io.jgitkins.web.application.port.out.RepositoryPort;
import io.jgitkins.web.infrastructure.util.PathUtils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class RepositoryManageService implements RepositoryManageUseCase {

	private static final Pattern BRANCH_NAME_PATTERN = Pattern.compile("^[A-Za-z0-9._/-]+$");
	private static final long MAX_UPLOAD_SIZE = 5L * 1024L * 1024L;
	private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
			".txt", ".md", ".java", ".kt", ".xml", ".yml", ".yaml", ".json", ".js", ".ts", ".css", ".html", ".gitkeep"
	);

	private final RepositoryPort repositoryPort;

	@Override
	public RepositoryBranchCreateResult createBranchByPath(String namespace,
														 String repoName,
														 String branchName,
														 String sourceBranch) {
		if (!StringUtils.hasText(branchName) || !BRANCH_NAME_PATTERN.matcher(branchName.trim()).matches()) {
			return new RepositoryBranchCreateResult(null, "유효한 브랜치 이름을 입력해 주세요.");
		}
		RepositorySummary repository = findRepository(namespace, repoName);
		if (repository == null || repository.id() == null) {
			return new RepositoryBranchCreateResult(null, "Repository not found.");
		}
		String from = StringUtils.hasText(sourceBranch) ? sourceBranch.trim() : "main";
		return repositoryPort.createBranch(repository.id(), branchName.trim(), from);
	}

	@Override
	public RepositoryFileUploadResult uploadFileByPath(String namespace,
													 String repoName,
													 String branch,
													 String path,
													 String message,
													 MultipartFile file) {
		RepositorySummary repository = findRepository(namespace, repoName);
		if (repository == null || repository.id() == null) {
			return new RepositoryFileUploadResult("Repository not found.");
		}
		if (!StringUtils.hasText(branch)) {
			return new RepositoryFileUploadResult("브랜치를 선택해 주세요.");
		}
		if (!StringUtils.hasText(path)) {
			return new RepositoryFileUploadResult("파일 경로를 입력해 주세요.");
		}
		if (!StringUtils.hasText(message)) {
			return new RepositoryFileUploadResult("커밋 메시지를 입력해 주세요.");
		}
		if (file == null || file.isEmpty()) {
			return new RepositoryFileUploadResult("업로드할 파일을 선택해 주세요.");
		}
		if (file.getSize() > MAX_UPLOAD_SIZE) {
			return new RepositoryFileUploadResult("파일 크기는 5MB 이하만 허용됩니다.");
		}
		if (!isAllowedExtension(file.getOriginalFilename())) {
			return new RepositoryFileUploadResult("허용되지 않는 파일 확장자입니다.");
		}

		try {
			RepositoryFileUploadRequest request = new RepositoryFileUploadRequest(
					repository.id(),
					branch.trim(),
					path.trim(),
					message.trim(),
					file.getOriginalFilename(),
					file.getContentType(),
					file.getBytes()
			);
			return repositoryPort.uploadFile(request);
		} catch (IOException ex) {
			return new RepositoryFileUploadResult("파일을 읽는 중 오류가 발생했습니다.");
		}
	}

	@Override
	public RepositoryFileUploadResult createDirectoryByPath(String namespace,
															 String repoName,
															 String branch,
															 String directoryPath,
															 String message) {
		RepositorySummary repository = findRepository(namespace, repoName);
		if (repository == null || repository.id() == null) {
			return new RepositoryFileUploadResult("Repository not found.");
		}
		if (!StringUtils.hasText(branch)) {
			return new RepositoryFileUploadResult("브랜치를 선택해 주세요.");
		}
		if (!StringUtils.hasText(directoryPath)) {
			return new RepositoryFileUploadResult("디렉터리 경로를 입력해 주세요.");
		}
		if (!StringUtils.hasText(message)) {
			return new RepositoryFileUploadResult("커밋 메시지를 입력해 주세요.");
		}

		String normalizedDirectory = directoryPath.trim().replace('\\', '/');
		normalizedDirectory = normalizedDirectory.replaceAll("^/+", "").replaceAll("/+$", "");
		if (!StringUtils.hasText(normalizedDirectory)) {
			return new RepositoryFileUploadResult("유효한 디렉터리 경로를 입력해 주세요.");
		}

		String gitkeepPath = normalizedDirectory + "/.gitkeep";
		RepositoryFileUploadRequest request = new RepositoryFileUploadRequest(
				repository.id(),
				branch.trim(),
				gitkeepPath,
				message.trim(),
				".gitkeep",
				"text/plain",
				"keep".getBytes(StandardCharsets.UTF_8)
		);
		return repositoryPort.uploadFile(request);
	}

	private boolean isAllowedExtension(String filename) {
		if (!StringUtils.hasText(filename)) {
			return false;
		}
		String normalized = filename.toLowerCase();
		for (String ext : ALLOWED_EXTENSIONS) {
			if (normalized.endsWith(ext)) {
				return true;
			}
		}
		return false;
	}

	private RepositorySummary findRepository(String namespace, String repoName) {
		if (!StringUtils.hasText(repoName)) {
			return null;
		}
		return repositoryPort.fetchRepositories().stream()
				.filter(item -> matchesRepository(item, namespace, repoName))
				.findFirst()
				.orElse(null);
	}

	private boolean matchesRepository(RepositorySummary repository, String namespace, String repoName) {
		if (repository == null) {
			return false;
		}
		RepositoryKey key = PathUtils.resolveRepositoryKey(repository.clonePath(), repository.path());
		if (key == null || !repoName.equalsIgnoreCase(key.repoName())) {
			return false;
		}
		if (!StringUtils.hasText(namespace)) {
			return true;
		}
		return namespace.equalsIgnoreCase(key.namespace())
				|| namespace.equalsIgnoreCase(PathUtils.lastSegment(key.namespace()));
	}
}
