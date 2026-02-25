package fr.dossierfacile.api.front.log;

import fr.dossierfacile.api.front.controller.UserController;
import fr.dossierfacile.api.front.exception.PasswordRecoveryTokenNotFoundException;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.UserService;
import fr.dossierfacile.common.config.GlobalExceptionHandler;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class GlobalExceptionHandlerTest {

    private MockMvc mvc;
    private final UserService userService = mock(UserService.class);

    @Nested
    class withoutGlobalExceptionHandler {
        @BeforeEach
        void setUp() {
            var controller = new UserController(userService, mock(AuthenticationFacade.class));
            mvc = MockMvcBuilders.standaloneSetup(controller).build();
        }

        @Test
        void shouldReturnA404() throws Exception {
            doThrow(new PasswordRecoveryTokenNotFoundException("invalid-token")).when(userService).createPassword(anyString(), anyString());
            mvc.perform(post("/api/user/createPassword/invalid-token")
                    .contentType("application/json")
                    .content("{\"password\":\"azerty\"}"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void shouldReturnA500() {
            doThrow(new ArrayIndexOutOfBoundsException()).when(userService).createPassword(anyString(), anyString());
            try {
                mvc.perform(post("/api/user/createPassword/valid-token")
                        .contentType("application/json")
                        .content("{\"password\":\"azerty\"}"));
            } catch (Exception e) {
                assertThat(e).isInstanceOf(ServletException.class);
            }
        }
    }

    @Nested
    class withGlobalExceptionHandler {
        @BeforeEach
        void setUp() {
            var controller = new UserController(userService, mock(AuthenticationFacade.class));
            mvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler()).build();
        }

        @Test
        void shouldReturnA404() throws Exception {
            doThrow(new PasswordRecoveryTokenNotFoundException("invalid-token")).when(userService).createPassword(anyString(), anyString());
            mvc.perform(post("/api/user/createPassword/invalid-token")
                    .contentType("application/json")
                    .content("{\"password\":\"azerty\"}"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void shouldReturnA500() throws Exception {
            doThrow(new ArrayIndexOutOfBoundsException()).when(userService).createPassword(anyString(), anyString());
            mvc.perform(post("/api/user/createPassword/valid-token")
                    .contentType("application/json")
                    .content("{\"password\":\"azerty\"}"))
                    .andExpect(status().isInternalServerError());
        }
    }
}
