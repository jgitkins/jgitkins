package io.jgitkins.server.identity.access.adapter.in.rest;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jgitkins.server.identity.access.adapter.in.support.RequesterUserIdResolver;
import io.jgitkins.server.identity.access.application.port.in.SignupUseCase;
import io.jgitkins.server.support.ErrorStatusMappingTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SignupController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({RequesterUserIdResolver.class, ErrorStatusMappingTestConfig.class})
class SignupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SignupUseCase signupUseCase;

    @Test
    void activate_callsUseCaseAndReturnsOk() throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of("username", "new_name"));

        mockMvc.perform(post("/api/signup/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .principal(() -> "42")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error").doesNotExist());

        verify(signupUseCase).activate(42L, "new_name");
    }

    @Test
    void activate_withMissingField_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/signup/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .principal(() -> "42")
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void activate_passesAuthenticatedRequesterId() throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of("username", "new_name"));

        mockMvc.perform(post("/api/signup/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .principal(() -> "0000000009")
                        .content(body))
                .andExpect(status().isOk());

        // Leading zeros are a valid decimal id, and the value the use case receives must be the parsed
        // number rather than the string it arrived as. Asserted because a controller that forwarded the
        // principal name verbatim would also make the previous test pass.
        verify(signupUseCase).activate(9L, "new_name");
    }

    @Test
    void activate_rejectsMalformedRequesterWithoutUseCaseInteraction() throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of("username", "new_name"));

        for (String malformed : java.util.List.of("0", "00", "-1", "+1", " 42", "42 ", "abc",
                "9999999999999999999999")) {
            mockMvc.perform(post("/api/signup/activate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .principal(() -> malformed)
                            .content(body))
                    .andExpect(status().isUnauthorized());
        }

        // Anonymous is the other rejection path and must behave the same way from the client's side.
        mockMvc.perform(post("/api/signup/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());

        // The point of the whole task: a broken credential must not reach the use case. If it did, the
        // first observable effect would be a database read for whatever id was salvaged from it.
        verifyNoInteractions(signupUseCase);
    }
}
