package io.jgitkins.server.presentation.api.web;

import io.jgitkins.server.application.dto.result.OrganizeCreationResult;
import io.jgitkins.server.application.port.in.OrganizeLoadUseCase;
import io.jgitkins.server.presentation.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Web Organize")
@RequestMapping("/api/internal/organizes")
public class WebOrganizeController {

	private final OrganizeLoadUseCase organizeLoadUseCase;

	@Operation(summary = "List Accessible Organizes (Web)")
	@GetMapping
	public ResponseEntity<ApiResponse<List<OrganizeCreationResult>>> getAccessibleOrganizes() {
		return ApiResponse.ok(organizeLoadUseCase.getAccessibleOrganizes());
	}
}
