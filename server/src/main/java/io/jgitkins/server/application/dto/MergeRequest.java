package io.jgitkins.server.application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MergeRequest {
    private String sourceBranch;
    private String targetBranch;
    private String commitMessage;
    private String authorName;
    private String authorEmail;
}
