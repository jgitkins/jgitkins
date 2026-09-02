package io.jgitkins.server.collaboration.adapter.in.web;

import io.jgitkins.server.collaboration.application.contract.OrganizeCreationResult;
import io.jgitkins.server.collaboration.application.port.in.OrganizeLoadUseCase;
import io.jgitkins.core.web.api.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.jgitkins.server.shared.application.security.AuthenticatedUser;
import io.jgitkins.server.shared.application.security.CurrentUser;

@RestController
@RequiredArgsConstructor
@Tag(name = "Web Organize")
@RequestMapping("/api/internal/organizes")
public class WebOrganizeController {

	private final OrganizeLoadUseCase organizeLoadUseCase;

	@Operation(summary = "List Accessible Organizes (Web)")
	@GetMapping
	public ResponseEntity<ApiResponse<List<OrganizeCreationResult>>> getAccessibleOrganizes(
			@CurrentUser AuthenticatedUser currentUser) {
		return ApiResponse.ok(organizeLoadUseCase.getAccessibleOrganizes(
				AuthenticatedUser.userIdOrNull(currentUser)));
	}
}
