package fr.dossierfacile.common.service;

import fr.dossierfacile.common.model.apartment_sharing.ApplicationModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestServiceImplTest {

    @Mock
    private RestTemplate restTemplate;

    private RequestServiceImpl requestService;

    @BeforeEach
    void setUp() {
        requestService = new RequestServiceImpl(restTemplate);
    }

    @Test
    void send_whenUrlIsForbiddenSsrfHost_shouldNotInvokeRestTemplate() {
        ApplicationModel model = new ApplicationModel();

        // SSRF attempts to localhost, 127.0.0.1, internal IPs, and AWS metadata
        requestService.send(model, "http://127.0.0.1/callback", "api-key");
        requestService.send(model, "http://localhost:8080/callback", "api-key");
        requestService.send(model, "http://169.254.169.254/latest/meta-data/", "api-key");
        requestService.send(model, "http://10.0.0.1/internal", "api-key");

        verify(restTemplate, never()).exchange(any(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void send_whenUrlIsValidPublicUrl_shouldInvokeRestTemplate() {
        ApplicationModel model = new ApplicationModel();
        String validUrl = "https://partner.example.com/webhook";

        when(restTemplate.exchange(eq(validUrl), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(org.springframework.http.ResponseEntity.ok("OK"));

        requestService.send(model, validUrl, "api-key");

        verify(restTemplate).exchange(eq(validUrl), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class));
    }
}
