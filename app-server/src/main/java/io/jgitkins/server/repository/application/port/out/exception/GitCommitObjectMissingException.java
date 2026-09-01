package io.jgitkins.server.repository.application.port.out.exception;

import lombok.Getter;

@Getter
public class GitCommitObjectMissingException extends GitPortException {

    private final String commitHash;

    public GitCommitObjectMissingException(String commitHash) {
        super("Git commit object is missing: " + commitHash);
        this.commitHash = commitHash;
    }

    public GitCommitObjectMissingException(String commitHash, Throwable cause) {
        super("Git commit object is missing: " + commitHash, cause);
        this.commitHash = commitHash;
    }

}
