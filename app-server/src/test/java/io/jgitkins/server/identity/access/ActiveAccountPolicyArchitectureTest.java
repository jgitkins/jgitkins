package io.jgitkins.server.identity.access;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.jgitkins.server.identity.access.application.port.out.ActiveAccountPolicyPort;
import io.jgitkins.server.identity.access.infrastructure.adapter.policy.ActiveAccountPolicyAdapter;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ActiveAccountPolicyArchitectureTest {
    private static final Path ROOT = Path.of("src/main/java/io/jgitkins/server/identity/access");

    @Test void portAndAdapterHaveExactOwnershipPackages() {
        assertEquals("io.jgitkins.server.identity.access.application.port.out", ActiveAccountPolicyPort.class.getPackageName());
        assertEquals("io.jgitkins.server.identity.access.infrastructure.adapter.policy", ActiveAccountPolicyAdapter.class.getPackageName());
        assertTrue(ActiveAccountPolicyPort.class.isAssignableFrom(ActiveAccountPolicyAdapter.class));
    }

    @Test void policySourcesDoNotImportForeignTechnologyOrContexts() throws Exception {
        String port = Files.readString(ROOT.resolve("application/port/out/ActiveAccountPolicyPort.java"));
        String adapter = Files.readString(ROOT.resolve("infrastructure/adapter/policy/ActiveAccountPolicyAdapter.java"));
        for (String forbidden : new String[]{"org.springframework.security", ".infrastructure.persistence.model", "io.jgitkins.server.collaboration.", "io.jgitkins.server.repository.", "io.jgitkins.server.execution.", "io.jgitkins.server.change.review."}) {
            assertFalse(adapter.contains(forbidden), forbidden);
        }
        for (String forbidden : new String[]{"domain.aggregate", "domain.vo", "infrastructure."}) {
            assertFalse(port.contains(forbidden), forbidden);
        }
    }
}
