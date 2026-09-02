/**
 * Collaborators this context's use cases lean on, which no use case is named after.
 *
 * <p>The distinction from the parent {@code service} package is whether an inbound port names it. A
 * class in {@code service} implements a {@code port/in} use case and an adapter reaches it. A class
 * here is reached only by those, and renaming or splitting one changes nothing outside this context.
 *
 * <p>This was {@code application/support} until the split. {@code support} named no rule -- it said
 * only that something did not fit elsewhere, which is how a package becomes a drawer. Everything in
 * it turned out to be behaviour: resolvers, assemblers, providers, allocators, builders, a facade.
 * Behaviour belongs under {@code service}; the word {@code internal} says which half.
 *
 * <p>The arguments and return values these take live in {@code application/contract/internal}.
 *
 * <p>{@code shared/application/support} was deliberately left where it is. The shared kernel has no
 * {@code service} package, so moving it would create one holding nothing but this.
 */
package io.jgitkins.server.identity.access.application.service.internal;
