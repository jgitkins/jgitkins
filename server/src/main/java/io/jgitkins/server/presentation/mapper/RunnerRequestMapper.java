package io.jgitkins.server.presentation.mapper;

import io.jgitkins.server.application.dto.command.RunnerRegisterCommand;
import io.jgitkins.server.presentation.dto.RunnerCreateRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RunnerRequestMapper {

    @Mapping(target = "description", source = "request.description")
    @Mapping(target = "scopeType", source = "request.scopeType")
    @Mapping(target = "targetId", source = "request.targetId")
    RunnerRegisterCommand toCommand(RunnerCreateRequest request);
}
