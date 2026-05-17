//package io.jgitkins.runner.presentation.consumer;
//
//import io.jgitkins.runner.application.port.in.JobExecuteCommand;
//import io.jgitkins.runner.application.port.in.JobExecuteUseCase;
//import io.jgitkins.runner.domain.ExecutionResult;
//import io.jgitkins.runner.presentation.dto.JobExecutionMessage;
//import io.jgitkins.runner.presentation.mapper.JobExecutionMapper;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.amqp.rabbit.annotation.RabbitListener;
//import org.springframework.stereotype.Component;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class JobExecutionConsumer {
//
//    private final JobExecuteUseCase jobExecuteUseCase;
//    private final JobExecutionMapper jobExecutionMapper;
//
//    @RabbitListener(queues = "jgitkins.task.queue")
//    public void receiveTask(JobExecutionMessage message) {
//        log.info("Received task message: {}", message);
//
////        try {
//        JobExecuteCommand command = jobExecutionMapper.toCommand(message);
//
//        ExecutionResult result = jobExecuteUseCase.execute(command);
//        log.info("Job execution completed with status: {}, exitCode: {}", result.getStatus(), result.getExitCode());
//
////        } catch (Exception e) {
////            log.error("Failed to execute job", e);
////        }
//    }
//}
