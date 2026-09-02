/**
 * Application-layer values that no use case exposes.
 *
 * <p>The axis is who can see the type, and it is answerable by reading a port interface:
 *
 * <ul>
 *   <li>{@code application/contract} -- a use case takes it or returns it. It appears in an
 *       {@code application/port/in} signature, so an inbound adapter binds to it and changing it is
 *       a change to this context's public face.
 *   <li>{@code application/internal} (here) -- no use case mentions it. Either an outbound port
 *       passes it, or it only ever flows between steps inside this context.
 * </ul>
 *
 * <p>The split was measured rather than guessed: every method signature under
 * {@code application/port/in} and {@code application/port/out} was parsed, and each type placed by
 * where it actually appears. Before that, {@code contract/result} held nine types, of which only
 * seven were use-case output; the rest were these.
 *
 * <p><strong>These cross the outbound port</strong>, so an adapter implements against them and
 * changing one breaks that adapter:
 *
 * <ul>
 *   <li>{@link BranchCreationContext}
 *   <li>{@link CommitFile}
 * </ul>
 *
 * <p>The others below flow only between steps in this context and no adapter depends on them. That
 * difference is real and the type names do not carry it, which is the one thing this flat package
 * gives up; the compiler still names the adapter that breaks.
 *
 * <p>{@code change/review} and {@code collaboration} have no package like this one. Measured, they
 * have zero types that no port mentions -- not an oversight, and not a reason to create an empty
 * package to make the five contexts look alike.
 */
package io.jgitkins.server.repository.application.internal;
