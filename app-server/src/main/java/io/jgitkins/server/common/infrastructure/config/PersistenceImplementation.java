package io.jgitkins.server.common.infrastructure.config;

import java.util.Optional;

/**
 * The persistence technology backing one capability slice.
 *
 * <p>Deliberately limited to two values. A capability is served by MyBatis or by JPA, never by both
 * at once: a dual-write path would make a cutover unrollbackable, because the two stores could
 * diverge with no single source of truth to fall back to.
 *
 * <p>{@link #MYBATIS} is the default everywhere until a slice has been migrated and its cutover
 * evidence recorded.
 */
public enum PersistenceImplementation {
    MYBATIS("mybatis"),
    JPA("jpa");

    private final String wireValue;

    PersistenceImplementation(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * The exact lowercase token accepted in configuration. Matching is exact on purpose: accepting
     * {@code MyBatis} or {@code JPA } as well would mean a typo silently resolves to a real
     * implementation, and a persistence cutover is the last place to be forgiving about input.
     */
    public String wireValue() {
        return wireValue;
    }

    static Optional<PersistenceImplementation> fromWireValue(String candidate) {
        for (PersistenceImplementation implementation : values()) {
            if (implementation.wireValue.equals(candidate)) {
                return Optional.of(implementation);
            }
        }
        return Optional.empty();
    }
}
