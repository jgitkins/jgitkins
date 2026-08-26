package io.jgitkins.server.common.infrastructure.config;

import io.jgitkins.server.common.infrastructure.exception.InvalidPersistenceSelectorException;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Resolves which {@link PersistenceImplementation} serves one capability slice.
 *
 * <pre>
 *   configuration property                                    this class                 outcome
 *   ----------------------------------------------------------------------------------------------
 *   absent / empty / blank                              -->   default                --> MYBATIS
 *   "mybatis"                                           -->   exact match            --> MYBATIS
 *   "jpa"                                               -->   exact match            --> JPA
 *   anything else ("Jpa", "mysql", "jpa ", "true", ...) -->   reject                 --> throws
 * </pre>
 *
 * <p>A pure function of (property name, raw value) with no Spring or context types. Two reasons it
 * stays that way:
 *
 * <ul>
 *   <li>This class lives under {@code common}, and
 *       {@code InfrastructureOwnershipArchitectureTest#commonForeignContextImportsAreExactlyAllowlisted}
 *       fails on any import of a foreign bounded context from {@code common} that is not in its
 *       exact-path allowlist. Returning a chosen adapter from here would drag a context type in and
 *       trip that gate, so resolution lives here and binding lives in the owning context.
 *   <li>Being a pure function makes the reject path trivially testable without booting a context,
 *       which matters because the reject path is the one that must never regress into a silent
 *       fallback.
 * </ul>
 *
 * <p>Rejecting rather than defaulting on an unknown value is the whole point. If a bad value fell
 * back to MyBatis, a deployment that believed it had cut over to JPA would keep writing through
 * MyBatis and report success, and the rollback evidence would be indistinguishable from the
 * cutover evidence.
 */
public final class PersistenceImplementationSelector {

    /** Applied when the property is absent, empty, or blank. */
    public static final PersistenceImplementation DEFAULT = PersistenceImplementation.MYBATIS;

    private PersistenceImplementationSelector() {
    }

    /**
     * Builds the canonical property name for a capability slice:
     * {@code jgitkins.persistence.<module-slug>.<capability-slug>.implementation}.
     *
     * <p>Centralised here so every slice in the 2.70-2.77 chain derives the same shape instead of
     * hand-writing the namespace and drifting.
     */
    public static String propertyName(String moduleSlug, String capabilitySlug) {
        return "jgitkins.persistence." + moduleSlug + "." + capabilitySlug + ".implementation";
    }

    /**
     * @param propertyName the property the value came from, used only to make the failure message
     *                     actionable
     * @param rawValue     the configured value, may be {@code null}
     * @throws InvalidPersistenceSelectorException if {@code rawValue} is present but not an exact
     *                                             match for a known implementation
     */
    public static PersistenceImplementation resolve(String propertyName, String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return DEFAULT;
        }
        return PersistenceImplementation.fromWireValue(rawValue)
                .orElseThrow(() -> new InvalidPersistenceSelectorException(propertyName, rawValue, allowedValues()));
    }

    private static String allowedValues() {
        return Arrays.stream(PersistenceImplementation.values())
                .map(PersistenceImplementation::wireValue)
                .collect(Collectors.joining(", "));
    }
}
