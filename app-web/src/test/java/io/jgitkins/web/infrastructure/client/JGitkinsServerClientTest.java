package io.jgitkins.web.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import io.jgitkins.web.application.dto.OrganizeFetchResult;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class JGitkinsServerClientTest {

    private HttpServer server;
    private JGitkinsServerClient client;

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/internal/organizes", exchange -> {
            String body = """
                    {"data":[{"id":3,"name":"org-c","description":null,"ownerId":7,"createdAt":"2026-01-01T00:00:00","updatedAt":null}],"error":null}
                    """;
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (var output = exchange.getResponseBody()) {
                output.write(bytes);
            }
        });
        server.start();
        client = new JGitkinsServerClient(
                RestClient.builder().baseUrl("http://127.0.0.1:" + server.getAddress().getPort()).build(),
                new ObjectMapper().findAndRegisterModules());
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void fetchAccessibleOrganizes_deserializesApiResponseDataAndAllFields() {
        OrganizeFetchResult result = client.fetchAccessibleOrganizes();

        assertThat(result.errorMessage()).isNull();
        assertThat(result.organizes()).singleElement().satisfies(organize -> {
            assertThat(organize.id()).isEqualTo(3L);
            assertThat(organize.name()).isEqualTo("org-c");
            assertThat(organize.description()).isNull();
            assertThat(organize.ownerId()).isEqualTo(7L);
            assertThat(organize.createdAt()).hasToString("2026-01-01T00:00");
            assertThat(organize.updatedAt()).isNull();
        });
    }
}
