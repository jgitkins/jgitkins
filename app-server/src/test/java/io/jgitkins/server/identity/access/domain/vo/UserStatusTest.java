package io.jgitkins.server.identity.access.domain.vo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class UserStatusTest {
    @Test void fromStringParsesAllStatuses() {
        assertEquals(UserStatus.ACTIVE, UserStatus.fromString("active"));
        assertEquals(UserStatus.PENDING, UserStatus.fromString("PENDING"));
        assertEquals(UserStatus.BLOCKED, UserStatus.fromString("BLOCKED"));
        assertEquals(UserStatus.DELETED, UserStatus.fromString("DELETED"));
        assertEquals(UserStatus.PENDING, UserStatus.fromString("PENDING_USERNAME"));
    }

    @Test void fromNullableDefaultsOnlyMissingValuesToActive() {
        assertEquals(UserStatus.ACTIVE, UserStatus.fromNullable(null));
        assertEquals(UserStatus.ACTIVE, UserStatus.fromNullable(""));
        assertThrows(IllegalArgumentException.class, () -> UserStatus.fromString(null));
        assertThrows(IllegalArgumentException.class, () -> UserStatus.fromString(" "));
        assertThrows(IllegalArgumentException.class, () -> UserStatus.fromString("UNKNOWN"));
    }
}
