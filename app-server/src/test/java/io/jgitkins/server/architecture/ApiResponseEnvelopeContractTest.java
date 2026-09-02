package io.jgitkins.server.architecture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.jgitkins.core.web.api.response.ApiResponse;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * Every public method on every controller answers {@code ResponseEntity<ApiResponse<...>>}.
 *
 * <p>Extracted from {@code ArchitecturePackageConventionTest} before that file was deleted. It
 * survived the deletion because it reads {@link ControllerInventory#ALL} rather than an inline
 * class list, so it is not a second copy of the package tree: move a controller and this rule still
 * examines it. The eleven placement assertions in that file did not have that property, which is
 * why they went and this stayed.
 *
 * <p>The inventory it reads used to be an inline {@code List.of} here, and nothing asserted the list
 * was complete -- {@code MergeController} appeared twice, short form and fully qualified, so
 * nineteen entries named eighteen classes. {@code ControllerAllowlistCompletenessTest} now holds
 * that list against the source tree.
 *
 * <p><strong>Known ceiling.</strong> The completeness guard scans for {@code @RestController} with a
 * pattern requiring an open paren or end of line, so a controller declared with its annotation and
 * {@code @RequestMapping} on one line is invisible to both the scan and the inventory -- the two
 * sets agree and the guard passes over it. This rule inherits that hole: a controller the inventory
 * never learned about is a controller whose envelope nobody checks. Tracked separately; the fix is
 * in the scanner pattern, not here.
 */
class ApiResponseEnvelopeContractTest {

    @Test
    void restAndWebApiControllers_returnApiResponseEnvelope() {
        List<Class<?>> controllerClasses = ControllerInventory.ALL;

        // An empty inventory would make the forEach below assert nothing and report success.
        assertFalse(controllerClasses.isEmpty(), () -> "ControllerInventory is empty; the rule examined nothing");

        controllerClasses.stream()
                .flatMap(controllerClass -> Stream.of(controllerClass.getDeclaredMethods()))
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .forEach(ApiResponseEnvelopeContractTest::assertReturnsApiResponseEntity);
    }

    private static void assertReturnsApiResponseEntity(Method method) {
        assertEquals(ResponseEntity.class, method.getReturnType(),
                () -> "API method must return ResponseEntity<ApiResponse<...>>: " + method);

        Type genericReturnType = method.getGenericReturnType();
        assertTrue(genericReturnType instanceof ParameterizedType,
                () -> "API method must declare generic response body: " + method);

        Type responseBodyType = ((ParameterizedType) genericReturnType).getActualTypeArguments()[0];
        assertTrue(responseBodyType instanceof ParameterizedType,
                () -> "API method response body must be ApiResponse<...>: " + method);
        assertEquals(ApiResponse.class, ((ParameterizedType) responseBodyType).getRawType(),
                () -> "API method response body must be ApiResponse<...>: " + method);
    }
}
