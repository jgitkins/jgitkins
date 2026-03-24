package io.jgitkins.server.presentation.mapper;

import io.jgitkins.server.application.dto.result.RunnerDetailResult;
import io.jgitkins.server.presentation.dto.RunnerResponse;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RunnerResponseMapper {

    @Mapping(target = "description", source = "description")
    RunnerResponse toResponse(RunnerDetailResult result);

    List<RunnerResponse> toResponses(List<RunnerDetailResult> results);
}
