/**
 * The typed shapes this context's application layer defines, grouped by which boundary they cross.
 *
 * <p>Read {@code contract} as "the shapes this layer declares", not as "this layer's public face".
 * That reading is what lets {@code internal} sit under it without contradiction, and it is written
 * down here because the alternative reading was argued twice while this structure was chosen. The
 * parent names the kind of thing; the position names the boundary.
 *
 * <pre>
 *   contract/            a use case takes it or returns it -- named in application/port/in
 *   contract/external/   an outbound port passes it        -- named in application/port/out
 *   contract/internal/   no port names it                  -- flows between steps in this context
 * </pre>
 *
 * <p>Command and result sit together here rather than in {@code command/} and {@code result/}
 * subpackages. The direction is already in the type name -- {@code ...Command} is an input,
 * {@code ...Result} an output -- and a package repeating what the suffix says buys a directory and
 * no information. What the suffix cannot tell you is which boundary the type crosses, so that is
 * what the packages are for.
 *
 * <p>One question places a new file: does a port signature name this type, and which port? That is
 * answerable by opening two directories, which is why this axis was chosen over naming things for
 * what they look like. It was applied by parsing all 141 method signatures under {@code port/in}
 * and {@code port/out} rather than by reading class names, and the two disagreed: what is now this
 * package held thirty four types of which seventeen were use-case output, and {@code RepositoryKey}
 * sat in a package called internal while {@code RepositoryLoadUseCase} returned it.
 *
 * <p>A context has only the children it needs. {@code change/review} and {@code collaboration} have
 * neither {@code external} nor {@code internal}, because measured they have zero types in those
 * positions. Creating them empty to make the five contexts look alike would put a directory in the
 * tree that answers no question.
 *
 * <p>The data in {@code internal} is what {@code service/internal} takes and returns. The two are a
 * pair: collaborators there, their arguments here.
 */
package io.jgitkins.server.collaboration.application.contract;
