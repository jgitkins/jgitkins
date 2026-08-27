package io.jgitkins.server.identity.access.application.port.in;

public interface SignupUseCase {
    /**
     * @param requesterUserId the authenticated caller, resolved by the inbound adapter. Explicit rather
     *     than read from a {@code CurrentUserPort} inside the service: the use case decides what the
     *     actor is allowed to do, and it cannot be tested or called for a specific actor if it also
     *     decides who the actor is.
     */
    void activate(Long requesterUserId, String username);
}
