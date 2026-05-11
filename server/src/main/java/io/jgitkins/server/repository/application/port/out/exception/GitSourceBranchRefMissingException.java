package io.jgitkins.server.repository.application.port.out.exception;

public class GitSourceBranchRefMissingException extends GitPortException {

    private final String branchName;

    public GitSourceBranchRefMissingException(String branchName) {
        super("Git source branch ref is missing: " + branchName);
        this.branchName = branchName;
    }

    public GitSourceBranchRefMissingException(String branchName, Throwable cause) {
        super("Git source branch ref is missing: " + branchName, cause);
        this.branchName = branchName;
    }

    public String getBranchName() {
        return branchName;
    }
}
