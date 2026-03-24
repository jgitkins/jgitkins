package io.jgitkins.server.presentation.api.rest;

import io.jgitkins.server.application.dto.result.RunnerActivateResult;
import io.jgitkins.server.application.dto.result.RunnerDetailResult;
import io.jgitkins.server.application.dto.command.RunnerRegisterCommand;
import io.jgitkins.server.application.dto.result.RunnerRegistrationResult;
import io.jgitkins.server.application.port.in.RunnerActivateUseCase;
import io.jgitkins.server.application.port.in.RunnerDeleteUseCase;
import io.jgitkins.server.application.port.in.RunnerLoadUseCase;
import io.jgitkins.server.application.port.in.RunnerRegisterUseCase;
import io.jgitkins.server.presentation.common.ApiResponse;
import io.jgitkins.server.presentation.dto.RunnerActivateRequest;
import io.jgitkins.server.presentation.dto.RunnerCreateRequest;
import io.jgitkins.server.presentation.dto.RunnerResponse;
import io.jgitkins.server.presentation.mapper.RunnerRequestMapper;
import io.jgitkins.server.presentation.mapper.RunnerResponseMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "Runner Management", description = "Runner registration and lifecycle APIs")
@RequestMapping({"/api/runners"})
public class RunnerController {

    private final RunnerRegisterUseCase runnerRegisterUseCase;
    private final RunnerLoadUseCase runnerLoadUseCase;
    private final RunnerDeleteUseCase runnerDeleteUseCase;
    private final RunnerActivateUseCase runnerActivateUseCase;

    private final RunnerRequestMapper runnerRequestMapper;
    private final RunnerResponseMapper runnerResponseMapper;

    @Operation(summary = "Register Runner", description = "Register a runner instance and receive an authentication token")
    @PostMapping
    public ResponseEntity<ApiResponse<RunnerRegistrationResult>> registerRunner(@Valid @RequestBody RunnerCreateRequest request) {
        RunnerRegisterCommand registerCommand = runnerRequestMapper.toCommand(request);
        RunnerRegistrationResult result = runnerRegisterUseCase.register(registerCommand);
        return ApiResponse.created(result.getRunnerId(), result);
    }

    @Operation(summary = "List Runners", description = "Retrieve all registered runners")
    @GetMapping
    public ResponseEntity<ApiResponse<List<RunnerResponse>>> getRunners() {
        List<RunnerDetailResult> results = runnerLoadUseCase.getRunners();
        return ApiResponse.ok(runnerResponseMapper.toResponses(results));
    }

    @Operation(summary = "Get Runner", description = "Retrieve a runner detail by id")
    @GetMapping("/{runnerId}")
    public ResponseEntity<ApiResponse<RunnerResponse>> getRunner(@PathVariable Long runnerId) {
        RunnerDetailResult result = runnerLoadUseCase.getRunner(runnerId);
        return ApiResponse.ok(runnerResponseMapper.toResponse(result));
    }

    @Operation(summary = "Delete Runner", description = "Delete a runner by id")
    @DeleteMapping("/{runnerId}")
    public ResponseEntity<ApiResponse<Void>> deleteRunner(@PathVariable Long runnerId) {
        runnerDeleteUseCase.deleteRunner(runnerId);
        return ApiResponse.noContent();
    }

    @Operation(summary = "Activate Runner", description = "Activate a runner and set it ONLINE")
    @PostMapping("/activate")
    public ResponseEntity<ApiResponse<RunnerActivateResult>> activateRunner(@Valid @RequestBody RunnerActivateRequest request,
                                                                            HttpServletRequest httpServletRequest) {
        String clientIp = extractClientIp(httpServletRequest);
        RunnerActivateResult result = runnerActivateUseCase.activate(request.token(), clientIp);
        return ApiResponse.ok(result);
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
