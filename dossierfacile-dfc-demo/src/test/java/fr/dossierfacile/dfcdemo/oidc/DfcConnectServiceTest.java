package fr.dossierfacile.dfcdemo.oidc;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.dossierfacile.dfcdemo.config.DfcDemoProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class DfcConnectServiceTest {

    private static final DfcDemoProperties PROPERTIES = new DfcDemoProperties(
            "https://demo.partner.test",
            "https://sso.dossierfacile.test",
            "https://api.dossierfacile.test",
            "dfconnect-demo",
            "top-secret",
            "webhook-key");

    private MockRestServiceServer mockServer;
    private DfcConnectService service;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);
        service = new DfcConnectService(restTemplate, new ObjectMapper(), PROPERTIES);
    }

    @Test
    void authorize_url_targets_keycloak_with_encoded_redirect_uri() {
        String url = service.authorizeUrl();

        assertThat(url).startsWith("https://sso.dossierfacile.test/auth/realms/dossier-facile/protocol/openid-connect/auth?")
                .contains("scope=openid")
                .contains("response_type=code")
                .contains("client_id=dfconnect-demo")
                .contains("redirect_uri=https%3A%2F%2Fdemo.partner.test%2Fdisplay");
    }

    @Test
    void exchange_code_posts_form_to_token_endpoint_and_returns_access_token() {
        MultiValueMap<String, String> expectedForm = new LinkedMultiValueMap<>();
        expectedForm.add("client_id", "dfconnect-demo");
        expectedForm.add("client_secret", "top-secret");
        expectedForm.add("code", "the-code");
        expectedForm.add("grant_type", "authorization_code");
        expectedForm.add("redirect_uri", "https://demo.partner.test/display");

        mockServer.expect(requestTo("https://sso.dossierfacile.test/auth/realms/dossier-facile/protocol/openid-connect/token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().formData(expectedForm))
                .andRespond(withSuccess("{\"access_token\":\"tok\",\"token_type\":\"Bearer\"}", MediaType.APPLICATION_JSON));

        assertThat(service.exchangeCode("the-code")).isEqualTo("tok");
        mockServer.verify();
    }

    @Test
    void exchange_code_fails_with_keycloak_answer_when_code_is_invalid() {
        mockServer.expect(requestTo("https://sso.dossierfacile.test/auth/realms/dossier-facile/protocol/openid-connect/token"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"invalid_grant\"}"));

        assertThatThrownBy(() -> service.exchangeCode("bad"))
                .isInstanceOfSatisfying(DfcApiException.class, e -> {
                    assertThat(e.getStep()).isEqualTo(DfcConnectService.STEP_TOKEN);
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getResponseBody()).contains("invalid_grant");
                });
    }

    @Test
    void fetch_profile_sends_bearer_token_and_pretty_prints_json() {
        mockServer.expect(requestTo("https://api.dossierfacile.test/dfc/tenant/profile"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer tok"))
                .andRespond(withSuccess("{\"connectedTenantId\":42,\"apartmentSharing\":{\"id\":7}}", MediaType.APPLICATION_JSON));

        String profile = service.fetchProfile("tok");

        assertThat(profile).contains("\"connectedTenantId\" : 42").contains(System.lineSeparator());
        mockServer.verify();
    }

    @Test
    void fetch_profile_fails_with_api_answer_when_token_is_rejected() {
        mockServer.expect(requestTo("https://api.dossierfacile.test/dfc/tenant/profile"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("unauthorized"));

        assertThatThrownBy(() -> service.fetchProfile("expired"))
                .isInstanceOfSatisfying(DfcApiException.class, e -> {
                    assertThat(e.getStep()).isEqualTo(DfcConnectService.STEP_PROFILE);
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(e.getResponseBody()).isEqualTo("unauthorized");
                });
    }

    @Test
    void logout_calls_api_with_bearer_token() {
        mockServer.expect(requestTo("https://api.dossierfacile.test/dfc/tenant/logout"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer tok"))
                .andRespond(withSuccess());

        service.logout("tok");
        mockServer.verify();
    }
}
