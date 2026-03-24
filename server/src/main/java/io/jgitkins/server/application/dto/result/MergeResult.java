package io.jgitkins.server.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MergeResult {

    public enum Status { MERGEABLE, MERGED, ALREADY_UP_TO_DATE, CONFLICTS, NO_COMMON_ANCESTOR }
    private Status status;
    private String targetBranch;
    private String sourceBranch;
    private String newCommitId;   // null if not merged
    private String resultTreeId;  // merged tree id
    private List<String> conflicts; // paths
}
