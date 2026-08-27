package io.jgitkins.server.identity.access.adapter.in.rest;

import io.jgitkins.server.identity.access.adapter.in.rest.dto.request.UserUsernameUpdateRequest;
import io.jgitkins.server.identity.access.adapter.in.support.RequesterUserIdResolver;
import io.jgitkins.server.identity.access.application.port.in.SignupUseCase;
import io.jgitkins.core.web.api.response.ApiResponse;
import io.jgitkins.server.shared.application.exception.UnauthenticatedException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Signup")
@RequestMapping("/api/signup")
public class SignupController {

    private final SignupUseCase signupUseCase;
    private final RequesterUserIdResolver requesterUserIdResolver;

    /**
     * Explicit constructor rather than {@code @RequiredArgsConstructor}.
     *
     * <p>The qualifier has to sit on the constructor parameter, and Lombok's generated constructor
     * cannot carry it. Two beans of type {@code RequesterUserIdResolver} exist — this context's and
     * collaboration's, which have deliberately different error semantics — so an unqualified injection
     * would either fail as ambiguous or, worse, resolve to whichever one the container preferred.
     */
    SignupController(SignupUseCase signupUseCase,
                     @Qualifier("identityRequesterUserIdResolver")
                     RequesterUserIdResolver requesterUserIdResolver) {
        this.signupUseCase = signupUseCase;
        this.requesterUserIdResolver = requesterUserIdResolver;
    }

    @Operation(summary = "Activate signup with username")
    @PostMapping("/activate")
    public ResponseEntity<ApiResponse<Void>> activate(@Valid @RequestBody UserUsernameUpdateRequest request,
                                                      Principal principal) {
        String principalName = principal == null ? null : principal.getName();
        // Resolution happens before the use case is touched. A malformed principal must not reach the
        // service at all: if it did, the first observable effect of a broken credential would be a
        // database read for whatever id was salvaged from it.
        Long requesterUserId = requesterUserIdResolver.resolve(principalName)
                .orElseThrow(() -> new UnauthenticatedException("Authentication required"));
        signupUseCase.activate(requesterUserId, request.username());
        return ApiResponse.ok();
    }
}
