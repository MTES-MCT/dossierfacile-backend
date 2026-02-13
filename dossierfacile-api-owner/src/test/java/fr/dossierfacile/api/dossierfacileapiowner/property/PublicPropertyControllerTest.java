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
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
}
