package io.jgitkins.server.presentation.api.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jgitkins.server.application.dto.RunnerExecutionConfig;
import io.jgitkins.server.application.dto.RunnerRuntimeConfig;
import io.jgitkins.server.application.dto.command.RunnerRegisterCommand;
import io.jgitkins.server.application.dto.result.RunnerActivateResult;
import io.jgitkins.server.application.dto.result.RunnerDetailResult;
import io.jgitkins.server.application.dto.result.RunnerRegistrationResult;
import io.jgitkins.server.application.port.in.RunnerActivateUseCase;
import io.jgitkins.server.application.port.in.RunnerDeleteUseCase;
import io.jgitkins.server.application.port.in.RunnerLoadUseCase;
import io.jgitkins.server.application.port.in.RunnerRegisterUseCase;
import io.jgitkins.server.domain.model.vo.RunnerScopeType;
import io.jgitkins.server.presentation.advice.GlobalExceptionHandler;
import io.jgitkins.server.presentation.advice.mapper.ApplicationErrorHttpStatusMapper;
import io.jgitkins.server.presentation.advice.mapper.CompositeErrorHttpStatusMapper;
import io.jgitkins.server.presentation.advice.mapper.DomainErrorHttpStatusMapper;
import io.jgitkins.server.presentation.advice.mapper.InfrastructureErrorHttpStatusMapper;
import io.jgitkins.server.presentation.dto.RunnerCreateRequest;
import io.jgitkins.server.presentation.dto.RunnerResponse;
import io.jgitkins.server.presentation.mapper.RunnerRequestMapper;
import io.jgitkins.server.presentation.mapper.RunnerResponseMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class RunnerControllerTest {

    @Mock
    private RunnerRegisterUseCase runnerRegisterUseCase;

    @Mock
    private RunnerLoadUseCase runnerLoadUseCase;

    @Mock
    private RunnerDeleteUseCase runnerDeleteUseCase;

    @Mock
    private RunnerActivateUseCase runnerActivateUseCase;

    @Mock
    private RunnerRequestMapper runnerRequestMapper;

    @Mock
    private RunnerResponseMapper runnerResponseMapper;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        CompositeErrorHttpStatusMapper statusMapper = new CompositeErrorHttpStatusMapper(
                List.of(
                        new DomainErrorHttpStatusMapper(),
                        new ApplicationErrorHttpStatusMapper(),
                        new InfrastructureErrorHttpStatusMapper()
                )
        );
        RunnerController controller = new RunnerController(
                runnerRegisterUseCase,
                runnerLoadUseCase,
                runnerDeleteUseCase,
                runnerActivateUseCase,
                runnerRequestMapper,
                runnerResponseMapper
        );

        this.mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler(statusMapper))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Test
    void registerRunner_returnsCreated() throws Exception {
        RunnerCreateRequest request = new RunnerCreateRequest("runner-01", RunnerScopeType.GLOBAL, null);

        RunnerRegisterCommand command = RunnerRegisterCommand.builder()
                .description("runner-01")
                .scopeType(RunnerScopeType.GLOBAL)
                .build();

        RunnerRegistrationResult result = RunnerRegistrationResult.builder()
                .runnerId(100L)
                .token("token-100")
                .status("OFFLINE")
                .registeredAt(LocalDateTime.now())
                .build();

        when(runnerRequestMapper.toCommand(any(RunnerCreateRequest.class))).thenReturn(command);
        when(runnerRegisterUseCase.register(command)).thenReturn(result);

        mockMvc.perform(post("/api/runners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/runners/100")))
                .andExpect(jsonPath("$.data.runnerId").value(100L))
                .andExpect(jsonPath("$.data.token").value("token-100"));

        verify(runnerRegisterUseCase).register(command);
    }

    @Test
    void getRunners_returnsMappedResponses() throws Exception {
        RunnerDetailResult detail = RunnerDetailResult.builder()
                .runnerId(1L)
                .description("runner")
                .status("ONLINE")
                .registeredAt(LocalDateTime.now())
                .build();
        RunnerResponse response = new RunnerResponse(1L, null, "runner", "ONLINE", null, null);

        when(runnerLoadUseCase.getRunners()).thenReturn(List.of(detail));
        when(runnerResponseMapper.toResponses(any())).thenReturn(List.of(response));

        mockMvc.perform(get("/api/runners"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].runnerId").value(1L))
                .andExpect(jsonPath("$.data[0].status").value("ONLINE"));

        verify(runnerLoadUseCase).getRunners();
    }

    @Test
    void getRunner_returnsSingleRunner() throws Exception {
        RunnerDetailResult detail = RunnerDetailResult.builder()
                .runnerId(2L)
                .description("runner-2")
                .status("OFFLINE")
                .registeredAt(LocalDateTime.now())
                .build();
        RunnerResponse response = new RunnerResponse(2L, null, "runner-2", "OFFLINE", null, null);

        when(runnerLoadUseCase.getRunner(2L)).thenReturn(detail);
        when(runnerResponseMapper.toResponse(detail)).thenReturn(response);

        mockMvc.perform(get("/api/runners/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.runnerId").value(2L))
                .andExpect(jsonPath("$.data.description").value("runner-2"));

        verify(runnerLoadUseCase).getRunner(2L);
    }

    @Test
    void deleteRunner_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/runners/9"))
                .andExpect(status().isNoContent());

        verify(runnerDeleteUseCase).deleteRunner(9L);
    }

    @Test
    void activateRunner_prefersXForwardedForHeader() throws Exception {
        RunnerActivateResult result = RunnerActivateResult.builder()
                .runtimeConfig(RunnerRuntimeConfig.builder()
                        .serviceHost("localhost")
                        .restScheme("http")
                        .restPort(8080)
                        .restBasePath("/api")
                        .grpcPort(6565)
                        .pollIntervalMs(5000L)
                        .busyWaitIntervalMs(1000L)
                        .build())
                .executionConfig(RunnerExecutionConfig.defaultConfig())
                .build();

        when(runnerActivateUseCase.activate("runner-token", "203.0.113.10")).thenReturn(result);

        mockMvc.perform(post("/api/runners/activate")
                        .header("X-Forwarded-For", "203.0.113.10, 10.0.0.1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"runner-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.runtimeConfig.serviceHost").value("localhost"))
                .andExpect(jsonPath("$.data.executionConfig.runnerImageName").value("jenkins/jenkinsfile-runner"));

        verify(runnerActivateUseCase).activate("runner-token", "203.0.113.10");
    }
}
