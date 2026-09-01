package io.jgitkins.server.common.infrastructure.error;

import io.jgitkins.core.common.problem.ProblemSpec;
import lombok.Getter;

@Getter
public enum InfrastructureProblemSpec implements ProblemSpec<InfrastructureErrorCode> {
    BRANCH_CREATE_FAILED(InfrastructureErrorCode.JGIT_OPERATION_FAILED, "JGIT-500-BRANCH-CREATE", "Branch creation failed", "jgit.branchCreateFailed"),
    BRANCH_DELETE_FAILED(InfrastructureErrorCode.JGIT_OPERATION_FAILED, "JGIT-500-BRANCH-DELETE", "Branch deletion failed", "jgit.branchDeleteFailed"),
    COMMIT_FAILED(InfrastructureErrorCode.JGIT_OPERATION_FAILED, "JGIT-500-COMMIT", "Commit operation failed", "jgit.commitFailed"),
    COMMIT_LOAD_FAILED(InfrastructureErrorCode.JGIT_OPERATION_FAILED, "JGIT-500-COMMIT-LOAD", "Commit load failed", "jgit.commitLoadFailed"),
    FILE_LOAD_FAILED(InfrastructureErrorCode.JGIT_OPERATION_FAILED, "JGIT-500-FILE-LOAD", "File load failed", "jgit.fileLoadFailed"),
    FILE_READ_FAILED(InfrastructureErrorCode.FILESYSTEM_ACCESS_FAILED, "FS-500-FILE-READ", "File read failed", "filesystem.fileReadFailed"),
    FILESYSTEM_ACCESS_FAILED(InfrastructureErrorCode.FILESYSTEM_ACCESS_FAILED, "FS-500-ACCESS", "Filesystem access failed", "filesystem.accessFailed"),
    HEAD_REFERENCE_RESOLVE_FAILED(InfrastructureErrorCode.JGIT_OPERATION_FAILED, "JGIT-500-HEAD-RESOLVE", "Head reference resolve failed", "jgit.headReferenceResolveFailed"),
    HEAD_REFERENCE_UPDATE_FAILED(InfrastructureErrorCode.JGIT_OPERATION_FAILED, "JGIT-500-HEAD-UPDATE", "Head reference update failed", "jgit.headReferenceUpdateFailed"),
    REPOSITORY_CREATE_FAILED(InfrastructureErrorCode.JGIT_OPERATION_FAILED, "JGIT-500-REPO-CREATE", "Repository creation failed", "jgit.repositoryCreateFailed"),
    REPOSITORY_DELETE_FAILED(InfrastructureErrorCode.FILESYSTEM_ACCESS_FAILED, "FS-500-REPO-DELETE", "Repository deletion failed", "filesystem.repositoryDeleteFailed"),
    RUNNER_ACTIVATION_FAILED(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED, "PERSIST-500-RUNNER-ACTIVE", "Runner activation persistence failed", "persistence.runnerActivationFailed"),
    RUNNER_DELETION_FAILED(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED, "PERSIST-500-RUNNER-DELETE", "Runner deletion persistence failed", "persistence.runnerDeletionFailed");

    private final InfrastructureErrorCode errorCode;
    private final String code;
    private final String defaultMessage;
    private final String messageKey;

    InfrastructureProblemSpec(InfrastructureErrorCode errorCode, String code, String defaultMessage, String messageKey) {
        this.errorCode = errorCode;
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.messageKey = messageKey;
    }

}
