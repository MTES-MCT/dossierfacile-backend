package fr.dossierfacile.api.dossierfacileapiowner.property;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.dossierfacile.api.dossierfacileapiowner.TestApplication;
import fr.dossierfacile.common.entity.Property;
import fr.dossierfacile.common.service.interfaces.LogService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PublicPropertyController.class)
@ActiveProfiles("test")
@ContextConfiguration(classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class PublicPropertyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PropertyService propertyService;

    @MockitoBean
    private OwnerPropertyMapper ownerPropertyMapper;

    @MockitoBean
    private LogService logService;

    private static final String TOKEN = "vJz1UPaeeHakWs90pQVJ";
    private static final String GET_PROPERTY_URL = "/api/property/public/" + TOKEN;

    @Nested
    @DisplayName("GET /api/property/public/{token}")
    class GetPropertyByToken {

        @Test
        @DisplayName("retourne 200 et le bien quand le token existe")
        void shouldReturn200AndPropertyWhenTokenExists() throws Exception {
            Property property = Property.builder()
                    .id(1L)
                    .token(TOKEN)
                    .name("Mon logement")
                    .build();

            LightPropertyModel expectedModel = LightPropertyModel.builder()
                    .id(property.getId())
                    .token(property.getToken())
                    .name(property.getName())
                    .build();

            when(propertyService.getPropertyByToken(TOKEN)).thenReturn(Optional.of(property));
            when(ownerPropertyMapper.toLightPropertyModel(any(Property.class))).thenReturn(expectedModel);

            mockMvc.perform(get(GET_PROPERTY_URL))
                    .andExpect(status().isOk())
                    .andExpect(content().json(objectMapper.writeValueAsString(expectedModel)));

            verify(propertyService).logAccess(property);
        }

        @Test
        @DisplayName("retourne 404 quand aucun bien n'existe pour le token")
        void shouldReturn404WhenPropertyNotFound() throws Exception {
            when(propertyService.getPropertyByToken(TOKEN)).thenReturn(Optional.empty());

            mockMvc.perform(get(GET_PROPERTY_URL))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/property/public/subscribe/{propertyToken}")
    class SubscribeTenantToProperty {

        private static final String PROPERTY_TOKEN = "BumxjHJJehU9hhI8zK";
        private static final String SUBSCRIBE_URL = "/api/property/public/subscribe/" + PROPERTY_TOKEN;

        @Test
        @DisplayName("retourne 200 quand l'abonnement réussit")
        void shouldReturn200WhenSubscribeSucceeds() throws Exception {
            SubscriptionApartmentSharingOfTenantForm form = new SubscriptionApartmentSharingOfTenantForm(true, "a-valid-kc-token");

            mockMvc.perform(post(SUBSCRIBE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(form)))
                    .andExpect(status().isOk());

            verify(propertyService).subscribeTenantToProperty(PROPERTY_TOKEN, form.getKcToken());
        }

        @Test
        @DisplayName("retourne 404 quand le bien n'existe pas")
        void shouldReturn404WhenPropertyNotFound() throws Exception {
            SubscriptionApartmentSharingOfTenantForm form = new SubscriptionApartmentSharingOfTenantForm(true, "a-valid-kc-token");
            doThrow(new NoSuchElementException()).when(propertyService).subscribeTenantToProperty(any(), any());

            mockMvc.perform(post(SUBSCRIBE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(form)))
                    .andExpect(status().isNotFound());
        }
    }
}
