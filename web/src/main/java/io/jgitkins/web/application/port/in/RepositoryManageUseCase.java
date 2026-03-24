package io.jgitkins.web.application.port.in;

import io.jgitkins.web.application.dto.RepositoryBranchCreateResult;
import io.jgitkins.web.application.dto.RepositoryFileUploadResult;
import org.springframework.web.multipart.MultipartFile;

public interface RepositoryManageUseCase {

	RepositoryBranchCreateResult createBranchByPath(String namespace, String repoName, String branchName, String sourceBranch);

	RepositoryFileUploadResult uploadFileByPath(String namespace,
											 String repoName,
											 String branch,
											 String path,
											 String message,
											 MultipartFile file);

	RepositoryFileUploadResult createDirectoryByPath(String namespace,
													 String repoName,
													 String branch,
													 String directoryPath,
													 String message);
}
