package io.jgitkins.server.repository.application.contract;

public record RepositoryPermission(String role, boolean writable, boolean member) {

    /**
     * Whether the caller may see the repository at all.
     *
     * <p>{@code member()} alone does not answer this, and reading it as if it did is a live bug
     * source. {@code GitRepositoryAccessService.decide} short-circuits public repositories with
     * {@code isPublic && userId == null} -- the ANONYMOUS caller only. An authenticated non-member
     * on a public repository falls through to {@link #none()}, so {@code member()} is false for
     * someone who may plainly read the repository. Logging in would hide a public repository.
     *
     * <p>Stated once here because the rule had drifted into three separate copies: the string-keyed
     * {@code canRead}, the aggregate-keyed deletion policy, and the read-model-keyed access
     * validator. The validator's copy was missing the public branch.
     */
    public boolean visibleOn(boolean isPublic) {
        return isPublic || member();
    }

    public static RepositoryPermission anonymous() {
        return new RepositoryPermission("ANONYMOUS", false, false);
    }

    public static RepositoryPermission none() {
        return new RepositoryPermission("NONE", false, false);
    }
}
