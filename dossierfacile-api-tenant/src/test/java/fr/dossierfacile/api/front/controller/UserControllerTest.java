package fr.dossierfacile.api.front.controller;

import fr.dossierfacile.api.front.TestApplication;
import fr.dossierfacile.api.front.exception.UserNotFoundException;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.UserService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@ActiveProfiles("test")
@ContextConfiguration(classes = {TestApplication.class})
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private AuthenticationFacade authenticationFacade;

    private final SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtTokenWithDossier = jwt().authorities(new SimpleGrantedAuthority("SCOPE_dossier"));

    @Test
    void shouldReturnNotFoundForInvalidUrl() throws Exception {
        mockMvc.perform(
                get("/api/user/invalidUrl").with(jwtTokenWithDossier)
        ).andDo(print()).andExpect(status().is(404));
    }

    @Nested
    class ForgotPasswordTests {

        @Test
        void shouldRespondForbiddenWhenNoJwtWithScopeDossier() throws Exception {
            mockMvc.perform(
                            post("/api/user/forgotPassword")
                                    .contentType("application/json")
                    ).andDo(print())
                    .andExpect(status().isForbidden());
        }

        @Test
        void shouldRespondBadRequestWhenNoEmailIsPassed() throws Exception {
            mockMvc.perform(
                            post("/api/user/forgotPassword")
                                    .contentType("application/json")
                                    .with(jwtTokenWithDossier)
            ).andExpect(status().isBadRequest())
            .andDo(print());
        }

        @Test
        void shouldRespondBadRequestWhenBadEmailIsPassed() throws Exception {
            mockMvc.perform(
                            post("/api/user/forgotPassword")
                                    .contentType("application/json")
                                    .content("{\"email\": \"test\"}")
                                    .with(jwtTokenWithDossier)
                    ).andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        void shouldRespondOkWhenValidEmailIsPassed() throws Exception {
            mockMvc.perform(
                            post("/api/user/forgotPassword")
                                    .contentType("application/json")
                                    .content("{\"email\": \"test@test.fr\"}")
                                    .with(jwtTokenWithDossier)
                    ).andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().bytes(new byte[0]));

        }

        @Test
        void shouldRespondUserNotFoundWhenValidEmailIsPassed() throws Exception {
            doThrow(new UserNotFoundException("test@test.fr")).when(userService).forgotPassword(any());
            mockMvc.perform(
                            post("/api/user/forgotPassword")
                                    .contentType("application/json")
                                    .content("{\"email\": \"test@test.fr\"}")
                                    .with(jwtTokenWithDossier)
                    ).andDo(print())
                    .andExpect(status().isNotFound());
        }

    }

    //end-section forgotPassword

}
