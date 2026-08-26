package io.jgitkins.server.common.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jgitkins.server.common.infrastructure.exception.InvalidPersistenceSelectorException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class PersistenceImplementationSelectorTest {

    private static final String PROPERTY = "jgitkins.persistence.app-server.some-capability.implementation";

    @Test
    void parsesMybatisAndJpaExactLowercaseOnly() {
        assertThat(PersistenceImplementationSelector.resolve(PROPERTY, "mybatis"))
                .isEqualTo(PersistenceImplementation.MYBATIS);
        assertThat(PersistenceImplementationSelector.resolve(PROPERTY, "jpa"))
                .isEqualTo(PersistenceImplementation.JPA);
    }

    /**
     * Case variants and padded values are rejected rather than normalised. A selector typo that
     * resolves to a real implementation is worse than a startup failure, because the deployment then
     * writes through a store nobody chose.
     */
    @ParameterizedTest
    @ValueSource(strings = {"MYBATIS", "MyBatis", "JPA", "Jpa", " jpa", "jpa ", "j pa", "mybatis;", "hibernate",
            "mysql", "true", "1", "none", "null"})
    void rejectsUnknownValueWithNamedException(String rawValue) {
        assertThatThrownBy(() -> PersistenceImplementationSelector.resolve(PROPERTY, rawValue))
                .isInstanceOf(InvalidPersistenceSelectorException.class)
                .hasMessageContaining(PROPERTY)
                .hasMessageContaining(rawValue)
                .hasMessageContaining("mybatis")
                .hasMessageContaining("jpa");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    void defaultsToMybatisWhenPropertyAbsent(String rawValue) {
        assertThat(PersistenceImplementationSelector.resolve(PROPERTY, rawValue))
                .isEqualTo(PersistenceImplementation.MYBATIS);
        assertThat(PersistenceImplementationSelector.DEFAULT).isEqualTo(PersistenceImplementation.MYBATIS);
    }

    @Test
    void buildsTheCanonicalPropertyNamespace() {
        assertThat(PersistenceImplementationSelector.propertyName("app-server", "organize-organize-member-reference"))
                .isEqualTo("jgitkins.persistence.app-server.organize-organize-member-reference.implementation");
    }
}
