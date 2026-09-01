package io.jgitkins.server.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.springframework.web.client.RestTemplate;

/**
 * Compares the generated OpenAPI document against a committed baseline, against a RUNNING server.
 *
 * <p><strong>The {@code assumeTrue} below is the last one in this module, and it is deliberately not
 * task 2.103's business.</strong> That task replaced the MariaDB reachability assumptions -- sixteen
 * classes that skipped when nothing was listening on 127.0.0.1:53306 -- with the Testcontainers
 * singleton in {@code JpaMariaDbTestSupport}, which calls {@code container.start()} unconditionally
 * and has no fallback: no Docker means those classes error, not skip. MariaDB-gated skips are at zero.
 *
 * <p>This one gates on {@code -Dopenapi.base-url} instead, and what it needs is not a database but an
 * app-server already serving {@code /v3/api-docs} on a known port. Removing the skip would mean
 * booting and holding a server for every {@code ./gradlew test}, which is a different decision with a
 * different cost, so it keeps the skip and says so here rather than being counted as leftover work.
 *
 * <p>To run it: {@code ./gradlew :app-server:test -Dopenapi.base-url=http://127.0.0.1:18080}.
 */
class CollaborationInboundOpenApiContractTest {

    private static final List<String> AFFECTED_PATHS = List.of(
            "/api/organizes",
            "/api/organizes/me",
            "/api/organizes/{organizeId}",
            "/api/organizes/{organizeId}/members",
            "/api/organizes/{organizeId}/members/{userId}",
            "/api/internal/organizes"
    );

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void generatedOpenApi_matchesPreChangeBaseline() throws Exception {
        String baseUrl = System.getProperty("openapi.base-url");
        Assumptions.assumeTrue(baseUrl != null && !baseUrl.isBlank(),
                "Set -Dopenapi.base-url=http://127.0.0.1:18080 to run the external OpenAPI contract gate");

        JsonNode generated = new RestTemplate()
                .getForObject(baseUrl + "/v3/api-docs", JsonNode.class);

        JsonNode baseline;
        try (InputStream input = getClass().getResourceAsStream(
                "/contracts/collaboration-inbound-openapi-baseline.json")) {
            assertThat(input).as("OpenAPI baseline resource").isNotNull();
            baseline = objectMapper.readTree(input);
        }

        assertThat(canonicalize(selectAffectedContract(generated)))
                .isEqualTo(canonicalize(baseline));
    }

    private JsonNode selectAffectedContract(JsonNode document) {
        ObjectNode selectedPaths = JsonNodeFactory.instance.objectNode();
        for (String path : AFFECTED_PATHS) {
            selectedPaths.set(path, document.path("paths").path(path));
        }

        Set<String> schemaNames = new LinkedHashSet<>();
        collectSchemaReferences(selectedPaths, schemaNames);
        Set<String> processed = new HashSet<>();
        while (!processed.containsAll(schemaNames)) {
            String schemaName = schemaNames.stream()
                    .filter(name -> !processed.contains(name))
                    .findFirst()
                    .orElseThrow();
            processed.add(schemaName);
            collectSchemaReferences(document.path("components").path("schemas").path(schemaName), schemaNames);
        }

        ObjectNode schemas = JsonNodeFactory.instance.objectNode();
        schemaNames.stream().sorted().forEach(name ->
                schemas.set(name, document.path("components").path("schemas").path(name)));

        ObjectNode components = JsonNodeFactory.instance.objectNode();
        components.set("schemas", schemas);
        ObjectNode result = JsonNodeFactory.instance.objectNode();
        result.set("paths", selectedPaths);
        result.set("components", components);
        return result;
    }

    private void collectSchemaReferences(JsonNode value, Set<String> schemaNames) {
        if (value == null || value.isMissingNode() || value.isNull()) {
            return;
        }
        if (value.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = value.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (field.getKey().equals("$ref") && field.getValue().isTextual()
                        && field.getValue().asText().startsWith("#/components/schemas/")) {
                    schemaNames.add(field.getValue().asText().substring("#/components/schemas/".length()));
                }
                collectSchemaReferences(field.getValue(), schemaNames);
            }
        } else if (value.isArray()) {
            for (JsonNode child : (ArrayNode) value) {
                collectSchemaReferences(child, schemaNames);
            }
        }
    }

    private JsonNode canonicalize(JsonNode value) {
        if (value.isObject()) {
            ObjectNode sorted = JsonNodeFactory.instance.objectNode();
            value.fields().forEachRemaining(entry -> sorted.set(entry.getKey(), canonicalize(entry.getValue())));
            ObjectNode result = JsonNodeFactory.instance.objectNode();
            List<String> names = new ArrayList<>();
            sorted.fieldNames().forEachRemaining(names::add);
            names.sort(String::compareTo);
            for (String name : names) {
                result.set(name, sorted.get(name));
            }
            return result;
        }
        if (value.isArray()) {
            ArrayNode array = JsonNodeFactory.instance.arrayNode();
            value.forEach(child -> array.add(canonicalize(child)));
            return array;
        }
        return value;
    }
}
