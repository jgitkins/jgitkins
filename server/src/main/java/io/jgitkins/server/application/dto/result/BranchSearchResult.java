package io.jgitkins.server.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 브랜치 조회 전용 DTO.
 * 도메인 Aggregate를 노출하지 않고 화면/외부 채널에 필요한 정보만 전달한다.
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BranchSearchResult {

    private Long repositoryId;
    private String name;
    private boolean locked;
    private boolean ciEnabled;
    private boolean defaultBranch;
}
