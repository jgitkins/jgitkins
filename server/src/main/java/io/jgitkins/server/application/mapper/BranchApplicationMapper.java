package io.jgitkins.server.application.mapper;

import io.jgitkins.server.repository.application.contract.result.BranchSearchResult;
import io.jgitkins.server.domain.Branch;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BranchApplicationMapper {

    BranchSearchResult toSearchResult(Branch branch);
}
