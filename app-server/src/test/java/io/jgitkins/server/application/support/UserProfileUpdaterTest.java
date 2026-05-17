package io.jgitkins.server.application.support;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.jgitkins.server.domain.model.User;
import io.jgitkins.server.domain.model.UserIdentity;
import io.jgitkins.server.domain.model.UserStatus;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class UserProfileUpdaterTest {

    private final UserProfileUpdater updater = new UserProfileUpdater();

    @Test
    void applyUserUpdates_returnsSameUserWhenNoChangesExceptLogin() {
        User user = User.createWithStatus("user", "user@example.com", "User", null, UserStatus.ACTIVE);
        LocalDateTime before = user.getLastLoginAt();

        User updated = updater.applyUserUpdates(user, "user@example.com", "User", null, LocalDateTime.now());

        assertNotSame(user, updated);
        assertTrue(updated.getLastLoginAt().isAfter(before));
    }

    @Test
    void updateIdentityIfChanged_returnsSameIdentityWhenNoChanges() {
        UserIdentity identity = UserIdentity.create(1L, "google", "sub", "a@b.com", true, "Name", null);

        UserIdentity updated = updater.updateIdentityIfChanged(identity, "a@b.com", true, "Name", null);

        assertSame(identity, updated);
    }

    @Test
    void updateIdentityIfChanged_returnsNewIdentityWhenChanged() {
        UserIdentity identity = UserIdentity.create(1L, "google", "sub", "a@b.com", true, "Name", null);

        UserIdentity updated = updater.updateIdentityIfChanged(identity, "new@b.com", true, "Name", null);

        assertNotSame(identity, updated);
    }
}
