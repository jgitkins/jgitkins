package io.jgitkins.server.application.mapper;

import io.jgitkins.server.application.dto.result.RunnerDetailResult;
import io.jgitkins.server.application.dto.result.RunnerRegistrationResult;
import io.jgitkins.server.domain.aggregate.Runner;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RunnerApplicationMapper {

    @Mapping(target = "runnerId", source = "id")
    @Mapping(target = "status", expression = "java(runner.getStatus().name())")
    @Mapping(target = "registeredAt", source = "createdAt")
    RunnerRegistrationResult toRegistrationResult(Runner runner);

    @Mapping(target = "runnerId", source = "id")
    @Mapping(target = "status", expression = "java(runner.getStatus().name())")
    @Mapping(target = "registeredAt", source = "createdAt")
    RunnerDetailResult toActivationResult(Runner runner);
}
