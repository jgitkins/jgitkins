package io.jgitkins.server.execution.adapter.in.rest.translator;

import io.jgitkins.server.execution.application.contract.result.RunnerDetailResult;
import io.jgitkins.server.execution.adapter.in.rest.contract.response.RunnerResponse;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RunnerResponseMapper {

    @Mapping(target = "description", source = "description")
    RunnerResponse toResponse(RunnerDetailResult result);

    List<RunnerResponse> toResponses(List<RunnerDetailResult> results);
}
