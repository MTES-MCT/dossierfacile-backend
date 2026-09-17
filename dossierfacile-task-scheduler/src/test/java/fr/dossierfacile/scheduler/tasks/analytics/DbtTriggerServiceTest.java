package fr.dossierfacile.scheduler.tasks.analytics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DbtTriggerServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Test
    @DisplayName("Ne doit rien faire si dbtWebhookUrl est vide")
    void should_do_nothing_when_webhook_url_empty() {
        AnalyticsProperties properties = new AnalyticsProperties();
        properties.setDbtWebhookUrl("");
        DbtTriggerService service = new DbtTriggerService(properties, restTemplate);

        service.trigger();

        verifyNoInteractions(restTemplate);
    }

    @Test
    @DisplayName("Doit envoyer un POST HTTP avec Token authorization et cause body si configuré")
    void should_send_post_with_token_and_cause() {
        AnalyticsProperties properties = new AnalyticsProperties();
        properties.setDbtWebhookUrl("https://pa476.us1.dbt.com/api/v2/accounts/123/jobs/456/run/");
        properties.setDbtWebhookToken("my-secret-token");
        DbtTriggerService service = new DbtTriggerService(properties, restTemplate);

        when(restTemplate.postForEntity(eq("https://pa476.us1.dbt.com/api/v2/accounts/123/jobs/456/run/"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>("ok", HttpStatus.OK));

        service.trigger();

        ArgumentCaptor<HttpEntity<String>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(eq("https://pa476.us1.dbt.com/api/v2/accounts/123/jobs/456/run/"), entityCaptor.capture(), eq(String.class));

        HttpEntity<String> captured = entityCaptor.getValue();
        assertThat(captured.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(captured.getHeaders().getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Token my-secret-token");
        assertThat(captured.getBody()).isEqualTo("{\"cause\": \"Triggered via API\"}");
    }

    @Test
    @DisplayName("Ne doit pas lever d'exception si le webhook dbt échoue (ne pas impacter la réplication)")
    void should_not_throw_if_webhook_call_fails() {
        AnalyticsProperties properties = new AnalyticsProperties();
        properties.setDbtWebhookUrl("https://dbt.example.com/api/v1/jobs/123/run");
        DbtTriggerService service = new DbtTriggerService(properties, restTemplate);

        when(restTemplate.postForEntity(any(), any(), eq(String.class)))
                .thenThrow(new RestClientException("500 Internal Server Error"));

        assertThatCode(service::trigger).doesNotThrowAnyException();
    }
}
