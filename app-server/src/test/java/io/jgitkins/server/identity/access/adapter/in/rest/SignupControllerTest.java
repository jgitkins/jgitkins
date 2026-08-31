package io.jgitkins.server.identity.access.adapter.in.rest;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
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
@Import({ErrorStatusMappingTestConfig.class})
class SignupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SignupUseCase signupUseCase;

    @org.junit.jupiter.api.AfterEach
    void clearAuthentication() {
        io.jgitkins.server.support.TestAuthentication.clear();
    }

    @Test
    void activate_callsUseCaseAndReturnsOk() throws Exception {
        io.jgitkins.server.support.TestAuthentication.authenticateAs(42L);
        String body = objectMapper.writeValueAsString(java.util.Map.of("username", "new_name"));

        mockMvc.perform(post("/api/signup/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error").doesNotExist());

        verify(signupUseCase).activate(42L, "new_name");
    }

    @Test
    void activate_withMissingField_returnsBadRequest() throws Exception {
        io.jgitkins.server.support.TestAuthentication.authenticateAs(42L);
        mockMvc.perform(post("/api/signup/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void activate_passesAuthenticatedRequesterId() throws Exception {
        // Leading zeros still resolve to the parsed number, asserted at the codec in JwtTokenCodecTest.
        // Here the principal is already typed, so the id arrives as 9 with no string in between.
        io.jgitkins.server.support.TestAuthentication.authenticateAs(9L);
        String body = objectMapper.writeValueAsString(java.util.Map.of("username", "new_name"));

        mockMvc.perform(post("/api/signup/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        // Leading zeros are a valid decimal id, and the value the use case receives must be the parsed
        // number rather than the string it arrived as. Asserted because a controller that forwarded the
        // principal name verbatim would also make the previous test pass.
        verify(signupUseCase).activate(9L, "new_name");
    }

    @Test
    void activate_rejectsAnAnonymousRequesterWithoutTouchingTheUseCase() throws Exception {
        // This test used to loop over "0", "00", "-1", "+1", " 42", "42 ", "abc" and an overflowing
        // digit string, feeding each as a principal name. None of them is expressible any more:
        // AuthenticatedUser takes a positive Long, so a malformed requester cannot reach a controller
        // to be rejected there. The rule did not go away -- it moved one layer up, to
        // JwtTokenCodec.parseSubject, where JwtTokenCodecTest asserts the same eight spellings and
        // twenty more against the token itself. That is the better place for it: the codec is on the
        // path of every authenticated request, while this controller was one of twelve.
        //
        // What remains testable here, and is tested, is the case a client can actually produce.
        String body = objectMapper.writeValueAsString(java.util.Map.of("username", "new_name"));
        io.jgitkins.server.support.TestAuthentication.clear();

        mockMvc.perform(post("/api/signup/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(signupUseCase);
    }
}
