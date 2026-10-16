package fr.dossierfacile.api.front.das.mesri;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FranceConnectTokenServiceImplTest {

    private static final Instant NOW = Instant.ofEpochSecond(1_792_155_026L);

    private HttpClient httpClient;
    private FranceConnectTokenServiceImpl service;

    @BeforeEach
    void setUp() {
        httpClient = mock(HttpClient.class);
        service = new FranceConnectTokenServiceImpl("http://localhost:8085/auth", "dossier-facile", httpClient,
                new ObjectMapper(), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @SuppressWarnings("unchecked")
    private void answer(int status, String body) throws Exception {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
    }

    @Test
    void readsStoredTokenFromBrokerEndpoint() throws Exception {
        answer(200, "{\"access_token\":\"fc-abc\",\"expires_in\":60,\"token_type\":\"Bearer\","
                + "\"scope\":\"openid mesri_admissions\",\"accessTokenExpiration\":" + (NOW.getEpochSecond() + 53) + "}");

        FranceConnectAccessToken token = service.getFranceConnectAccessToken("user-jwt");

        assertThat(token.accessToken()).isEqualTo("fc-abc");
        assertThat(token.scope()).isEqualTo("openid mesri_admissions");
        assertThat(token.secondsLeft(NOW)).isEqualTo(53);

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(captor.capture(), any());
        assertThat(captor.getValue().uri().toString()).isEqualTo("http://localhost:8085/auth/realms/dossier-facile/broker/oidc/token");
        assertThat(captor.getValue().headers().firstValue("Authorization")).contains("Bearer user-jwt");
    }

    @Test
    void expiredTokenIsRejectedWithoutCallingApiParticulier() throws Exception {
        answer(200, "{\"access_token\":\"fc-abc\",\"accessTokenExpiration\":" + (NOW.getEpochSecond() - 1) + "}");

        assertThatThrownBy(() -> service.getFranceConnectAccessToken("user-jwt"))
                .isInstanceOf(FranceConnectTokenUnavailableException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void forbiddenMeansMissingReadTokenRole() throws Exception {
        answer(403, "{\"errorMessage\":\"Client not authorized to retrieve tokens from identity provider [oidc].\"}");

        assertThatThrownBy(() -> service.getFranceConnectAccessToken("user-jwt"))
                .isInstanceOf(FranceConnectTokenUnavailableException.class)
                .hasMessageContaining("403");
    }

    @Test
    void emptyBodyMeansNoTokenStored() throws Exception {
        answer(200, "{}");

        assertThatThrownBy(() -> service.getFranceConnectAccessToken("user-jwt"))
                .isInstanceOf(FranceConnectTokenUnavailableException.class);
    }
}
