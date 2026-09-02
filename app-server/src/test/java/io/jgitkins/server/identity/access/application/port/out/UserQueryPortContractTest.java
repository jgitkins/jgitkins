package io.jgitkins.server.identity.access.application.port.out;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.jgitkins.server.identity.access.application.contract.external.UserQueryResult;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class UserQueryPortContractTest {
    @Test
    void exposesOnlyReadQueries() {
        Set<String> methods = Arrays.stream(UserQueryPort.class.getDeclaredMethods())
                .map(Method::getName)
                .collect(Collectors.toSet());
        assertEquals(Set.of("findAll", "findUserDetailsById", "findUserIdByUsername", "findUsernameById", "existsByUsername"), methods);
        assertFalse(methods.contains("save"));
        assertTrue(Arrays.stream(UserQueryPort.class.getDeclaredMethods())
                .noneMatch(method -> Arrays.stream(method.getGenericReturnType().getTypeName().split("[<> ,]"))
                        .anyMatch(type -> type.endsWith(".User"))));
    }

    @Test
    void detailedQueryReturnsProjection() throws NoSuchMethodException {
        Method method = UserQueryPort.class.getDeclaredMethod("findUserDetailsById", Long.class);
        assertTrue(method.getGenericReturnType().getTypeName().contains(UserQueryResult.class.getSimpleName()));
    }
}
