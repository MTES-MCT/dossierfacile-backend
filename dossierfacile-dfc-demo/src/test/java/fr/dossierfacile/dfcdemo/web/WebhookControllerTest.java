package fr.dossierfacile.dfcdemo.web;

import fr.dossierfacile.dfcdemo.config.DfcDemoProperties;
import fr.dossierfacile.dfcdemo.webhook.WebhookLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WebhookController.class)
// Slices do not apply @ConfigurationPropertiesScan: register the settings explicitly
@EnableConfigurationProperties(DfcDemoProperties.class)
@TestPropertySource(properties = "dfc-demo.webhook-api-key=secret")
class WebhookControllerTest {

    private static final String BODY = "{\"partnerCallBackType\":\"CREATED_ACCOUNT\",\"onTenantId\":1}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WebhookLogRepository repository;

    @Test
    void stores_raw_body_when_api_key_is_valid() throws Exception {
        mockMvc.perform(post("/webhook")
                .header("x-api-key", "secret")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY))
                .andExpect(status().isOk())
                .andExpect(content().string("Nouvel enregistrement créé avec succès."));

        verify(repository).insert(BODY);
    }

    @Test
    void rejects_wrong_api_key() throws Exception {
        mockMvc.perform(post("/webhook")
                .header("x-api-key", "wrong")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY))
                .andExpect(status().isForbidden());

        verifyNoInteractions(repository);
    }

    @Test
    void rejects_missing_api_key() throws Exception {
        mockMvc.perform(post("/webhook").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());

        verifyNoInteractions(repository);
    }

    @Test
    void rejects_empty_body() throws Exception {
        mockMvc.perform(post("/webhook").header("x-api-key", "secret").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(repository);
    }
}
