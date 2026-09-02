package io.jgitkins.web.application.port.in.facade;

import io.jgitkins.web.application.contract.RepositoryBranchCreateResult;
import io.jgitkins.web.application.contract.RepositoryCreateContext;
import io.jgitkins.web.application.contract.RepositoryCreateRequest;
import io.jgitkins.web.application.contract.RepositoryCreateResult;
import io.jgitkins.web.application.contract.RepositoryDetailData;
import io.jgitkins.web.application.contract.RepositoryFileIndexEntry;
import io.jgitkins.web.presentation.support.RepositoryUserProfile;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface RepositoryFacadeUseCase {

    RepositoryCreateContext getInitData(RepositoryUserProfile profile, String ownerType, Long organizeId);

    RepositoryCreateResult createRepository(RepositoryCreateRequest request);

    RepositoryDetailData getRepositoryDetail(String namespace, String repoName, String branch, String directory,
            boolean authenticated);

    List<RepositoryFileIndexEntry> getFileIndex(String namespace, String repoName, String branch);

    RepositoryBranchCreateResult createBranch(String namespace, String repoName, String branchName, String sourceBranch,
            String currentBranch);

    RepositoryCreateResult uploadFile(String namespace, String repoName, String branch, String path, String message,
            MultipartFile file);

    RepositoryCreateResult createDirectory(String namespace, String repoName, String branch, String directoryPath,
            String message);
}
