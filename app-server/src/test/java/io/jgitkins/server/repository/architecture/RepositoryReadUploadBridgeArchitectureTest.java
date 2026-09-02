package io.jgitkins.server.repository.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import io.jgitkins.server.repository.application.contract.RepositoryKey;
import io.jgitkins.server.repository.application.port.in.RepositoryLoadUseCase;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * The upload bridge stays actor-neutral, and the metadata read stays actor-aware.
 *
 * <p>Task 2.64 introduced {@code resolveRepositoryKey} so the ID-based upload route could learn two
 * strings without depending on the full read contract. Task 2.65 then gave every metadata read an
 * explicit requester. Those two must not converge: if the upload bridge acquired a requester it would
 * start applying read authorization to a route that already did its own commit check, and if the
 * metadata read lost one it would go back to authorizing ambiently.
 *
 * <p>Asserted on the signatures, because the two are one careless refactor apart — "these look similar,
 * let's merge them" is a plausible change that no behavioural test would obviously catch.
 */
class RepositoryReadUploadBridgeArchitectureTest {

    private static final Path CONTROLLER = resolveRoot()
            .resolve("adapter/in/rest/RepositoryContentController.java");

    @Test
    void preservesActorNeutralUploadBridge() throws Exception {
        Method bridge = RepositoryLoadUseCase.class.getMethod("resolveRepositoryKey", Long.class);

        assertThat(bridge.getReturnType())
                .as("empty rather than throwing, so the 404 stays the controller's decision and the "
                        + "existing status is preserved")
                .isEqualTo(Optional.class);
        assertThat(bridge.getParameterTypes())
                .as("the bridge takes a repository id and nothing else. Adding a requester would apply "
                        + "read authorization to an upload route that already performs its own commit "
                        + "check, and the second check would deny writers who are not readers.")
                .containsExactly(Long.class);
        assertThat(bridge.getGenericReturnType().getTypeName())
                .contains(RepositoryKey.class.getName());
    }

    @Test
    void theMetadataReadStaysActorAware() throws Exception {
        Method metadataRead = Arrays.stream(RepositoryLoadUseCase.class.getMethods())
                .filter(method -> method.getName().equals("loadRepository"))
                .findFirst()
                .orElseThrow();

        assertThat(metadataRead.getParameterTypes())
                .as("loadRepository authorizes, so it must take the requester. The two methods look "
                        + "similar and are one careless merge apart.")
                .containsExactly(Long.class, Long.class);
    }

    @Test
    void theControllerNoLongerDerivesTheKeyItself() throws IOException {
        String source = Files.readString(CONTROLLER);

        assertThat(source)
                .as("the private derivation was replaced by the application boundary; leaving both would "
                        + "give the route two definitions of which path git actually serves")
                .doesNotContain("private RepositoryKey resolveRepositoryKey");
        assertThat(source)
                .as("and the route must use the neutral bridge rather than the full metadata read")
                .contains("repositoryLoadUseCase.resolveRepositoryKey(");
    }

    private static Path resolveRoot() {
        Path local = Path.of("src/main/java/io/jgitkins/server/repository");
        return Files.isDirectory(local)
                ? local
                : Path.of("app-server/src/main/java/io/jgitkins/server/repository");
    }
}
