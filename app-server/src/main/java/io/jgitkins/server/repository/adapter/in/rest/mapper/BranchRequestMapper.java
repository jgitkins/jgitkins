package io.jgitkins.server.repository.adapter.in.rest.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import io.jgitkins.server.repository.application.contract.command.BranchCreateCommand;
import io.jgitkins.server.repository.adapter.in.rest.dto.request.BranchCreateRequest;

@Mapper(componentModel = "spring")
public interface BranchRequestMapper {

    @Mapping(target = "requesterUserId", source = "requesterUserId")
    @Mapping(target = "repositoryId", source = "repositoryId")
    @Mapping(target = "branchName", source = "request.branchName")
    @Mapping(target = "sourceBranch", source = "request.sourceBranch")
    @Mapping(target = "physicalCreationRequired", constant = "true")
    BranchCreateCommand toCommand(Long requesterUserId, Long repositoryId,
                                  BranchCreateRequest request);
}
