package io.jgitkins.server.repository.application.contract.internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class BranchInfo {

    private String name;
    private String fullRef;
    private String commitId;
}
