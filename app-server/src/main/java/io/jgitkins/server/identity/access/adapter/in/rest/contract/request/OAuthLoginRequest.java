package io.jgitkins.server.identity.access.adapter.in.rest.contract.request;

import jakarta.validation.constraints.NotBlank;

/**
 * An OAuth login request: which provider, and the id token that provider issued.
 *
 * <p><strong>What this record used to hold, and why it does not.</strong> Until task 2.114 it carried
 * {@code subject}, {@code email}, {@code name}, {@code emailVerified} and {@code avatarUrl} as plain
 * fields, and the server used them as the identity of the person logging in. The route is
 * {@code permitAll} — it has to be, the caller has no token yet — so anyone could post an email
 * address and be handed a JWT for whichever account held it. The endpoint is app-web's, and app-web
 * had verified those claims before sending them; nothing made that true of any other caller.
 *
 * <p>So the fields are gone rather than constrained. Adding {@code @NotBlank} to {@code subject}
 * would have made the takeover well-formed. What the server needs is not a better-shaped claim but a
 * checkable one, and the id token is exactly that: signed by the provider, addressed to us, with an
 * expiry. Every identity field is now read out of it after
 * {@code OidcIdTokenVerifierAdapter} checks the signature, issuer, audience and expiry.
 *
 * <p>The two fields that remain do carry constraints, and {@code BoundaryValidationTest} holds this
 * route to them. A {@code @Valid} on a record with no constraints validates nothing while looking
 * like it does — the state this endpoint was in.
 */
public record OAuthLoginRequest(

        @NotBlank(message = "provider is required")
        String provider,

        @NotBlank(message = "idToken is required")
        String idToken
) {
}
