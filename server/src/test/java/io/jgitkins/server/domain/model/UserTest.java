package io.jgitkins.server.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.jgitkins.server.domain.exception.UserAlreadyActivatedException;
import io.jgitkins.server.domain.model.vo.Username;
import org.junit.jupiter.api.Test;

class UserTest {

    @Test
    void activateWithUsername_requiresUsername() {
        User pending = User.createWithStatus("temp", "a@b.com", "User", null, UserStatus.PENDING).withId(1L);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> pending.activateWithUsername(null));

        assertEquals("Username is required", exception.getMessage());
    }

    @Test
    void activateWithUsername_throwsDomainExceptionWhenAlreadyActivated() {
        User activeUser = User.createWithStatus("active_user", "a@b.com", "User", null, UserStatus.ACTIVE).withId(1L);

        assertThrows(UserAlreadyActivatedException.class,
                () -> activeUser.activateWithUsername(Username.from("new_name")));
    }
}
