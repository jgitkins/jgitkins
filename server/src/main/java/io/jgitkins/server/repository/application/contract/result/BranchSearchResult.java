package io.jgitkins.server.repository.application.contract.result;

/**
 * 브랜치 조회 전용 DTO.
 * 도메인 Aggregate를 노출하지 않고 화면/외부 채널에 필요한 정보만 전달한다.
 */
public record BranchSearchResult(
        Long repositoryId,
        String name,
        boolean locked,
        boolean ciEnabled,
        boolean defaultBranch
) {
}
