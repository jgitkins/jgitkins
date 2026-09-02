package io.jgitkins.server.execution.adapter.in.rest.translator;

import io.jgitkins.server.execution.application.contract.RunnerRegisterCommand;
import io.jgitkins.server.execution.adapter.in.rest.contract.request.RunnerCreateRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RunnerRequestMapper {

    @Mapping(target = "description", source = "request.description")
    @Mapping(target = "scopeType", source = "request.scopeType")
    @Mapping(target = "targetId", source = "request.targetId")
    RunnerRegisterCommand toCommand(RunnerCreateRequest request);
}
