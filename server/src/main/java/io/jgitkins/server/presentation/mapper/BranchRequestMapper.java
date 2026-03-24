package io.jgitkins.server.presentation.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import io.jgitkins.server.application.dto.command.BranchCreateCommand;
import io.jgitkins.server.presentation.dto.BranchCreateRequest;

@Mapper(componentModel = "spring")
public interface BranchRequestMapper {

    @Mapping(target = "repositoryId", source = "repositoryId")
    @Mapping(target = "branchName", source = "request.branchName")
    @Mapping(target = "sourceBranch", source = "request.sourceBranch")
    @Mapping(target = "physicalCreationRequired", constant = "true")
    BranchCreateCommand toCommand(Long repositoryId, BranchCreateRequest request);
}
