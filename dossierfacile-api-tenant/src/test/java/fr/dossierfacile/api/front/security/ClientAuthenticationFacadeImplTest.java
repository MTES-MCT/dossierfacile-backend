package fr.dossierfacile.api.front.security;

import fr.dossierfacile.api.front.exception.ClientNotFoundException;
import fr.dossierfacile.api.front.security.interfaces.ClientAuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.UserApiService;
import fr.dossierfacile.common.entity.UserApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static fr.dossierfacile.authentification.JwtFactoryKt.getDummyJwtWithCustomClaims;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class ClientAuthenticationFacadeImplTest {

    private static final UserApiService userApiService = mock(UserApiService.class);

    @Autowired
    private ClientAuthenticationFacade clientAuthenticationFacade;

    @TestConfiguration
    static class AuthentificationFacadeImplTestContextConfiguration {

        @Bean
        public ClientAuthenticationFacade getClientAuthentificationFacade() {
            return new ClientAuthenticationFacadeImpl(userApiService);
        }
    }

    @BeforeEach
    void before() {
        reset(userApiService);
    }

    @Nested
    class GetKeycloakClientIdTest {

        @Test
        void shouldReturnClientIdFromJwt() {
            var claimsMap = Map.of("client_id", "clientId");
            var jwt = getDummyJwtWithCustomClaims(claimsMap);
            SecurityContextHolder.setContext(new SecurityContextImpl(new JwtAuthenticationToken(jwt, Collections.emptyList())));
            var result = clientAuthenticationFacade.getKeycloakClientId();
            assertThat(result).isEqualTo("clientId");
        }

        @Test
        void shouldReturnClientIdFromJwtAlternativeClaim() {
            var claimsMap = Map.of("clientId", "clientId");
            var jwt = getDummyJwtWithCustomClaims(claimsMap);
            SecurityContextHolder.setContext(new SecurityContextImpl(new JwtAuthenticationToken(jwt, Collections.emptyList())));
            var result = clientAuthenticationFacade.getKeycloakClientId();
            assertThat(result).isEqualTo("clientId");
        }

        @Test
        void shouldReturnNullWhenClientIdIsNotDefined() {
            var jwt = getDummyJwtWithCustomClaims(Collections.emptyMap());
            SecurityContextHolder.setContext(new SecurityContextImpl(new JwtAuthenticationToken(jwt, Collections.emptyList())));
            var result = clientAuthenticationFacade.getKeycloakClientId();
            assertThat(result).isNull();
        }
    }

    @Nested
    class GetClientTest {

        @Test
        void shouldReturnClient() {
            var userApi = new UserApi();
            when(userApiService.findByName("clientId")).thenReturn(Optional.of(userApi));

            var claimsMap = Map.of("client_id", "clientId");
            var jwt = getDummyJwtWithCustomClaims(claimsMap);
            SecurityContextHolder.setContext(new SecurityContextImpl(new JwtAuthenticationToken(jwt, Collections.emptyList())));

            var result = clientAuthenticationFacade.getClient();
            assertThat(result).isEqualTo(userApi);
        }

        @Test
        void shouldThrowClientNotFoundException() {
            when(userApiService.findByName("clientId")).thenReturn(Optional.empty());

            var claimsMap = Map.of("client_id", "clientId");
            var jwt = getDummyJwtWithCustomClaims(claimsMap);
            SecurityContextHolder.setContext(new SecurityContextImpl(new JwtAuthenticationToken(jwt, Collections.emptyList())));

            assertThrows(ClientNotFoundException.class, () -> clientAuthenticationFacade.getClient());
        }
    }

    @Nested
    class IsClientTest {

        @Test
        void shouldReturnTrueWhenClientIdIsPresent() {
            var claimsMap = Map.of("client_id", "clientId");
            var jwt = getDummyJwtWithCustomClaims(claimsMap);
            SecurityContextHolder.setContext(new SecurityContextImpl(new JwtAuthenticationToken(jwt, Collections.emptyList())));

            var result = clientAuthenticationFacade.isClient();
            assertThat(result).isTrue();
        }

        @Test
        void shouldReturnFalseWhenClientIdIsNotPresent() {
            var jwt = getDummyJwtWithCustomClaims(Collections.emptyMap());
            SecurityContextHolder.setContext(new SecurityContextImpl(new JwtAuthenticationToken(jwt, Collections.emptyList())));

            var result = clientAuthenticationFacade.isClient();
            assertThat(result).isFalse();
        }
    }

    @Nested
    class GetApiVersionTest {

        @Test
        void shouldReturnApiVersion() {
            var userApi = new UserApi();
            userApi.setVersion(1);
            when(userApiService.findByName("clientId")).thenReturn(Optional.of(userApi));

            var claimsMap = Map.of("client_id", "clientId");
            var jwt = getDummyJwtWithCustomClaims(claimsMap);
            SecurityContextHolder.setContext(new SecurityContextImpl(new JwtAuthenticationToken(jwt, Collections.emptyList())));

            var result = clientAuthenticationFacade.getApiVersion();
            assertThat(result)
                    .isPresent()
                    .contains(1);
        }

        @Test
        void shouldReturnEmptyWhenClientNotFound() {
            when(userApiService.findByName("clientId")).thenReturn(Optional.empty());

            var claimsMap = Map.of("client_id", "clientId");
            var jwt = getDummyJwtWithCustomClaims(claimsMap);
            SecurityContextHolder.setContext(new SecurityContextImpl(new JwtAuthenticationToken(jwt, Collections.emptyList())));

            var result = clientAuthenticationFacade.getApiVersion();
            assertThat(result).isEmpty();
        }
    }
}