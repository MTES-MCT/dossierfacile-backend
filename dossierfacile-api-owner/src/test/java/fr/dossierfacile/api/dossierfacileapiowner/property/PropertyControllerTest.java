package fr.dossierfacile.api.dossierfacileapiowner.property;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.dossierfacile.api.dossierfacileapiowner.TestApplication;
import fr.dossierfacile.api.dossierfacileapiowner.register.AuthenticationFacade;
import fr.dossierfacile.api.dossierfacileapiowner.user.OwnerMapper;
import fr.dossierfacile.common.service.interfaces.LogService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(controllers = PropertyController.class)
@ActiveProfiles("test")
@ContextConfiguration(classes = {
        TestApplication.class,
    }
)
@AutoConfigureMockMvc(addFilters = false)
class PropertyControllerTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LogService logService;

    @MockitoBean
    private PropertyService propertyService;

    @MockitoBean
    private PropertyApartmentSharingService propertyApartmentSharingService;

    @MockitoBean
    private AuthenticationFacade authenticationFacade;

    @MockitoBean
    private OwnerMapper ownerMapper;

    @MockitoBean
    private PropertyMapper propertyMapper;

    record PropertyTestPayload(String name, String dpeDate) {
    }


    static Stream<Arguments> providePropertyData() {
        var localDate = LocalDate.now();
        var futureDate = localDate.plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        var currentDate = localDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        return Stream.of(
                Arguments.of(Named.of("Should return BadRequest when empty body sent", null), 400),
                Arguments.of(Named.of("Should return BadRequest when invalid date format (EEEE dd MMMM yyyy) is sent in body", new PropertyTestPayload("Test", "Lundi 15 mars 2021")), 400),
                Arguments.of(Named.of("Should return BadRequest when invalid date format (MM/dd/yyyy) is sent in body", new PropertyTestPayload("Test", "02/10/2025")), 400),
                Arguments.of(Named.of("Should return BadRequest when invalid date format (dd/MM/yyyy) is sent in body", new PropertyTestPayload("Test", "10/02/2025")), 400),
                Arguments.of(Named.of("Should return BadRequest when a specific date is sent in body", new PropertyTestPayload("Test", "52021-04-06")), 400),
                Arguments.of(Named.of("Should return BadRequest when a futur date is sent in body", new PropertyTestPayload("Test", futureDate)), 400),
                Arguments.of(Named.of("Should return 200 when valid body sent", new PropertyTestPayload("Test", "2021-08-01")), 200),
                Arguments.of(Named.of("Should return 200 when the current date is sent in body", new PropertyTestPayload("Test", currentDate)), 200)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("providePropertyData")
    @DisplayName("Test Create or Update Property")
    void testCreateOrUpdateProperty(PropertyTestPayload testPayload, int expectedStatus) throws Exception {
        if (expectedStatus == 200) {
            var propertyModel = new PropertyModel();
            propertyModel.setId(1L);
            propertyModel.setName("Test");
            propertyModel.setDpeDate("2021-08-01");
            when(propertyService.createOrUpdate(any())).thenReturn(propertyModel);
        }

        Supplier<PropertyForm> propertyForm = () -> {
            if (testPayload == null) {
                return null;
            } else {
                var tmpProperty = new PropertyForm();
                tmpProperty.setName(testPayload.name());
                tmpProperty.setDpeDate(testPayload.dpeDate());
                return tmpProperty;
            }
        };

        var mockMvcConfiguration = post("/api/property")
                .contentType("application/json");

        if (propertyForm.get() != null) {
            mockMvcConfiguration.content(objectMapper.writeValueAsString(propertyForm.get()));
        }

        mockMvc.perform(mockMvcConfiguration).andExpect(status().is(expectedStatus));
    }
}
