package io.jgitkins.runner.presentation.mapper;

import io.jgitkins.runner.application.port.in.JobExecuteCommand;
import io.jgitkins.runner.presentation.dto.JobExecutionMessage;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface JobExecutionMapper {
    JobExecuteCommand toCommand(JobExecutionMessage message);
}
