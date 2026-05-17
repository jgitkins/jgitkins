package io.jgitkins.server.repository.application.port.out.exception;

public class GitBranchRefMissingException extends GitPortException {

    private final String branchName;

    public GitBranchRefMissingException(String branchName) {
        super("Git branch ref is missing: " + branchName);
        this.branchName = branchName;
    }

    public String getBranchName() {
        return branchName;
    }
}
