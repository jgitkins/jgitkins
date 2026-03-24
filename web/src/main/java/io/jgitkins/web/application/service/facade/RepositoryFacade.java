package io.jgitkins.web.application.service.facade;

import io.jgitkins.web.application.dto.*;
import io.jgitkins.web.application.port.in.RepositoryCreateUseCase;
import io.jgitkins.web.application.port.in.RepositoryDetailUseCase;
import io.jgitkins.web.application.port.in.RepositoryManageUseCase;
import io.jgitkins.web.application.port.in.facade.RepositoryFacadeUseCase;
import io.jgitkins.web.application.port.out.OrganizePort;
import io.jgitkins.web.presentation.support.RepositoryUserProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RepositoryFacade implements RepositoryFacadeUseCase {

    private final RepositoryCreateUseCase repositoryCreateUseCase;
    private final RepositoryDetailUseCase repositoryDetailUseCase;
    private final RepositoryManageUseCase repositoryManageUseCase;
    private final OrganizePort organizePort;

    @Override
    public RepositoryCreateContext getInitData(RepositoryUserProfile profile, String ownerType, Long organizeId) {
        OrganizeFetchResult result = organizePort.fetchOrganizes();
        String ownerLabel = resolveOwnerLabel(profile, ownerType, organizeId, result.organizes());
        String ownerSlug = resolveOwnerSlug(profile, ownerType, organizeId, result.organizes());

        return new RepositoryCreateContext(
                result.organizes(),
                result.errorMessage(),
                profile,
                ownerLabel,
                ownerSlug);
    }

    @Override
    public RepositoryCreateResult createRepository(RepositoryCreateRequest request) {
        return repositoryCreateUseCase.createRepository(request);
    }

    @Override
    public RepositoryDetailData getRepositoryDetail(String namespace, String repoName, String branch, String directory,
            boolean authenticated) {
        RepositoryDetailData detail = repositoryDetailUseCase.loadRepositoryByPath(namespace, repoName, branch,
                directory);

        if (requiresNotFound(detail, authenticated)) {
            return null;
        }

        return detail;
    }

    @Override
    public List<RepositoryFileIndexEntry> getFileIndex(String namespace, String repoName, String branch) {
        return repositoryDetailUseCase.loadRepositoryFileIndexByPath(namespace, repoName, branch);
    }

    @Override
    public RepositoryBranchCreateResult createBranch(String namespace, String repoName, String branchName,
            String sourceBranch, String currentBranch) {
        String baseBranch = StringUtils.hasText(sourceBranch) ? sourceBranch : currentBranch;
        return repositoryManageUseCase.createBranchByPath(namespace, repoName, branchName, baseBranch);
    }

    @Override
    public RepositoryCreateResult uploadFile(String namespace, String repoName, String branch, String path,
            String message, MultipartFile file) {
        var result = repositoryManageUseCase.uploadFileByPath(namespace, repoName, branch, path, message, file);
        return new RepositoryCreateResult(null, result.errorMessage());
    }

    @Override
    public RepositoryCreateResult createDirectory(String namespace, String repoName, String branch,
            String directoryPath, String message) {
        var result = repositoryManageUseCase.createDirectoryByPath(namespace, repoName, branch, directoryPath, message);
        return new RepositoryCreateResult(null, result.errorMessage());
    }

    private boolean requiresNotFound(RepositoryDetailData detail, boolean authenticated) {
        if (detail == null || detail.repository() == null) {
            return false;
        }
        boolean isPublic = detail.repository().visibility() != null
                && "PUBLIC".equalsIgnoreCase(detail.repository().visibility());
        return !isPublic && !authenticated;
    }

    private String resolveOwnerLabel(RepositoryUserProfile profile, String ownerType, Long organizeId,
            List<OrganizeSummary> organizes) {
        if ("ORGANIZATION".equalsIgnoreCase(ownerType) && organizeId != null) {
            return organizes.stream()
                    .filter(o -> o.id().equals(organizeId))
                    .map(OrganizeSummary::name)
                    .findFirst()
                    .orElse("Unknown Organization");
        }
        return profile.name() != null ? profile.name() : profile.email();
    }

    private String resolveOwnerSlug(RepositoryUserProfile profile, String ownerType, Long organizeId,
            List<OrganizeSummary> organizes) {
        if ("ORGANIZATION".equalsIgnoreCase(ownerType) && organizeId != null) {
            return organizes.stream()
                    .filter(o -> o.id().equals(organizeId))
                    .map(OrganizeSummary::name)
                    .findFirst()
                    .orElse("unknown");
        }
        return profile.name() != null ? profile.name() : "user";
    }
}
