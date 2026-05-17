package io.jgitkins.server.repository.application.contract.result;

public record RepositoryPermission(String role, boolean writable, boolean member) {

    public static RepositoryPermission anonymous() {
        return new RepositoryPermission("ANONYMOUS", false, false);
    }

    public static RepositoryPermission none() {
        return new RepositoryPermission("NONE", false, false);
    }
}
