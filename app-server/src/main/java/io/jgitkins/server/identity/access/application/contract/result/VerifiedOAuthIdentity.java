package io.jgitkins.server.identity.access.application.contract.result;

/**
 * An identity read out of a token whose signature, issuer, audience and expiry we checked.
 *
 * <p>Every field here came from that token. Nothing on this record was supplied by the caller except
 * indirectly, by presenting a token the provider signed — which is the difference between this type
 * and the request DTO it replaced.
 *
 * @param emailVerified the provider's own claim, absent treated as false. Read by the account-linking
 *     rule: an unverified address must not attach a new provider identity to an existing account,
 *     because anyone able to assert an address at some provider could then become that account.
 */
public record VerifiedOAuthIdentity(
        String provider,
        String subject,
        String email,
        boolean emailVerified,
        String name,
        String avatarUrl
) {
}
