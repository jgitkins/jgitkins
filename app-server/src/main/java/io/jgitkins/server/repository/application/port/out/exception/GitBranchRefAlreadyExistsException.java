package io.jgitkins.server.repository.application.port.out.exception;

public class GitBranchRefAlreadyExistsException extends GitPortException {

    private final String branchName;

    public GitBranchRefAlreadyExistsException(String branchName) {
        super("Git branch ref already exists: " + branchName);
        this.branchName = branchName;
    }

    public String getBranchName() {
        return branchName;
    }
}
