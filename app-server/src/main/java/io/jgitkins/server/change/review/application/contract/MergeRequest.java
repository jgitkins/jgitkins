package io.jgitkins.server.change.review.application.contract;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
/**
 * Constrained against {@code BranchName}'s rule rather than a value object of its own: this context
 * passes branch names through as raw strings, and a null one becomes the literal ref
 * {@code refs/heads/null} at {@code MergeGitAdapter:84}.
 *
 * <p>{@code commitMessage}, {@code authorName}, and {@code authorEmail} are left unconstrained because
 * no rule in this context rejects them.
 */
public class MergeRequest {
    @NotBlank(message = "sourceBranch must not be blank")
    private String sourceBranch;
    @NotBlank(message = "targetBranch must not be blank")
    private String targetBranch;
    private String commitMessage;
    private String authorName;
    private String authorEmail;
}
