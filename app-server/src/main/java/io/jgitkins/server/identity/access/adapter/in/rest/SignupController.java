package io.jgitkins.server.identity.access.adapter.in.rest;

import io.jgitkins.server.identity.access.adapter.in.rest.dto.request.UserUsernameUpdateRequest;
import io.jgitkins.server.identity.access.application.port.in.SignupUseCase;
import io.jgitkins.core.web.api.response.ApiResponse;
import io.jgitkins.server.shared.application.exception.UnauthenticatedException;
import io.jgitkins.server.shared.application.security.AuthenticatedUser;
import io.jgitkins.server.shared.application.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Signup")
@RequestMapping("/api/signup")
public class SignupController {

    private final SignupUseCase signupUseCase;

    @Operation(summary = "Activate signup with username")
    @PostMapping("/activate")
    public ResponseEntity<ApiResponse<Void>> activate(@Valid @RequestBody UserUsernameUpdateRequest request,
                                                      @CurrentUser AuthenticatedUser currentUser) {
        // This route took java.security.Principal and pulled a string out of it, which was the only
        // shape of the round trip left in the codebase. The name-to-number step is gone: the token
        // codec parses the subject once, strictly, and hands over a typed requester.
        //
        // Rejected before the use case is touched. A missing credential must not produce a database
        // read for whatever id was salvaged from it.
        if (currentUser == null) {
            throw new UnauthenticatedException("Authentication required");
        }
        signupUseCase.activate(currentUser.userId(), request.username());
        return ApiResponse.ok();
    }
}
