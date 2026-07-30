package fr.dossierfacile.api.dossierfacileapiowner.property;

import fr.dossierfacile.api.dossierfacileapiowner.log.OwnerLogService;
import fr.dossierfacile.api.dossierfacileapiowner.mail.MailService;
import fr.dossierfacile.api.dossierfacileapiowner.register.AuthenticationFacade;
import fr.dossierfacile.common.entity.Owner;
import fr.dossierfacile.common.entity.Property;
import fr.dossierfacile.common.enums.OwnerLogType;
import fr.dossierfacile.common.model.AdemeResultModel;
import fr.dossierfacile.common.repository.PropertyLogRepository;
import fr.dossierfacile.common.service.interfaces.AdemeApiService;
import fr.dossierfacile.common.service.interfaces.TenantCommonService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.AdditionalAnswers;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


class PropertyServiceImplTest {

    // Create a new property when propertyForm.id is null
    @Test
    public void test_create_new_property_when_id_is_null() throws InterruptedException {
        // Arrange
        AuthenticationFacade authenticationFacade = mock(AuthenticationFacade.class);
        PropertyRepository propertyRepository = mock(PropertyRepository.class);
        OwnerPropertyMapper propertyMapper = mock(OwnerPropertyMapper.class);
        PropertyApartmentSharingService propertyApartmentSharingService = mock(PropertyApartmentSharingService.class);
        TenantCommonService tenantService = mock(TenantCommonService.class);
        PropertyLogRepository propertyLogRepository = mock(PropertyLogRepository.class);
        OwnerLogService ownerLogService = mock(OwnerLogService.class);
        MailService mailService = mock(MailService.class);
        JwtDecoder tenantJwtDecoder = mock(JwtDecoder.class);
        AdemeApiService ademeApiService = mock(AdemeApiService.class);

        PropertyServiceImpl propertyService = new PropertyServiceImpl(authenticationFacade, propertyRepository, propertyMapper, propertyApartmentSharingService, tenantService, propertyLogRepository, ownerLogService, mailService, ademeApiService);
        ReflectionTestUtils.setField(propertyService, "tenantJwtDecoder", tenantJwtDecoder);

        Owner owner = new Owner();
        owner.setId(1L);
        when(authenticationFacade.getOwner()).thenReturn(owner);

        PropertyForm propertyForm = new PropertyForm();
        propertyForm.setName("Test Property");

        Property property = new Property();
        when(propertyRepository.save(any(Property.class))).thenReturn(property);
        when(propertyMapper.toPropertyModel(any(Property.class))).thenReturn(new PropertyModel());

        // Act
        PropertyModel result = null;
        result = propertyService.createOrUpdate(propertyForm);

        // Assert
        assertNotNull(result);
        verify(ownerLogService).saveLog(OwnerLogType.PROPERTY_CREATED, owner.getId());
    }

    // Handle null values for optional fields in propertyForm
    @Test
    public void test_handle_null_values_for_optional_fields() throws InterruptedException {
        // Arrange
        AuthenticationFacade authenticationFacade = mock(AuthenticationFacade.class);
        PropertyRepository propertyRepository = mock(PropertyRepository.class);
        OwnerPropertyMapper propertyMapper = mock(OwnerPropertyMapper.class);
        PropertyApartmentSharingService propertyApartmentSharingService = mock(PropertyApartmentSharingService.class);
        TenantCommonService tenantService = mock(TenantCommonService.class);
        PropertyLogRepository propertyLogRepository = mock(PropertyLogRepository.class);
        OwnerLogService ownerLogService = mock(OwnerLogService.class);
        MailService mailService = mock(MailService.class);
        JwtDecoder tenantJwtDecoder = mock(JwtDecoder.class);
        AdemeApiService ademeApiService = mock(AdemeApiService.class);

        PropertyServiceImpl propertyService = new PropertyServiceImpl(authenticationFacade, propertyRepository, propertyMapper, propertyApartmentSharingService, tenantService, propertyLogRepository, ownerLogService, mailService, ademeApiService);
        ReflectionTestUtils.setField(propertyService, "tenantJwtDecoder", tenantJwtDecoder);

        Owner owner = new Owner();
        owner.setId(1L);
        when(authenticationFacade.getOwner()).thenReturn(owner);

        PropertyForm propertyForm = new PropertyForm();
        propertyForm.setId(1L);

        Property existingProperty = new Property();
        existingProperty.setId(1L);
        when(propertyRepository.findByIdAndOwnerId(1L, 1L)).thenReturn(Optional.of(existingProperty));
        when(propertyRepository.save(any(Property.class))).thenReturn(existingProperty);
        when(propertyMapper.toPropertyModel(any(Property.class))).thenReturn(new PropertyModel());

        // Act
        PropertyModel result = null;
        result = propertyService.createOrUpdate(propertyForm);

        // Assert
        assertNotNull(result);
    }

    @Test
    public void test_does_not_call_ademe_api_when_ademe_number_is_unchanged() throws Exception {
        // Arrange
        AuthenticationFacade authenticationFacade = mock(AuthenticationFacade.class);
        PropertyRepository propertyRepository = mock(PropertyRepository.class);
        OwnerPropertyMapper propertyMapper = mock(OwnerPropertyMapper.class);
        PropertyApartmentSharingService propertyApartmentSharingService = mock(PropertyApartmentSharingService.class);
        TenantCommonService tenantService = mock(TenantCommonService.class);
        PropertyLogRepository propertyLogRepository = mock(PropertyLogRepository.class);
        OwnerLogService ownerLogService = mock(OwnerLogService.class);
        MailService mailService = mock(MailService.class);
        JwtDecoder tenantJwtDecoder = mock(JwtDecoder.class);
        AdemeApiService ademeApiService = mock(AdemeApiService.class);

        PropertyServiceImpl propertyService = new PropertyServiceImpl(authenticationFacade, propertyRepository, propertyMapper, propertyApartmentSharingService, tenantService, propertyLogRepository, ownerLogService, mailService, ademeApiService);
        ReflectionTestUtils.setField(propertyService, "tenantJwtDecoder", tenantJwtDecoder);

        Owner owner = new Owner();
        owner.setId(1L);
        when(authenticationFacade.getOwner()).thenReturn(owner);

        Property existingProperty = new Property();
        existingProperty.setId(1L);
        existingProperty.setAdemeNumber("2337E0363555K");
        existingProperty.setEnergyConsumption(150);

        PropertyForm propertyForm = new PropertyForm();
        propertyForm.setId(1L);
        propertyForm.setAdemeNumber("2337E0363555K");
        propertyForm.setRentCost(587.0);

        when(propertyRepository.findByIdAndOwnerId(1L, 1L)).thenReturn(Optional.of(existingProperty));
        when(propertyRepository.save(any(Property.class))).thenAnswer(AdditionalAnswers.returnsFirstArg());
        when(propertyMapper.toPropertyModel(any(Property.class))).thenReturn(new PropertyModel());

        // Act
        PropertyModel result = propertyService.createOrUpdate(propertyForm);

        // Assert
        assertNotNull(result);
        verifyNoInteractions(ademeApiService);
        assertEquals(587.0, existingProperty.getRentCost());
        assertEquals("2337E0363555K", existingProperty.getAdemeNumber());
        assertEquals(150, existingProperty.getEnergyConsumption());
    }

    @Test
    public void test_calls_ademe_api_when_ademe_number_changes() throws Exception {
        // Arrange
        AuthenticationFacade authenticationFacade = mock(AuthenticationFacade.class);
        PropertyRepository propertyRepository = mock(PropertyRepository.class);
        OwnerPropertyMapper propertyMapper = mock(OwnerPropertyMapper.class);
        PropertyApartmentSharingService propertyApartmentSharingService = mock(PropertyApartmentSharingService.class);
        TenantCommonService tenantService = mock(TenantCommonService.class);
        PropertyLogRepository propertyLogRepository = mock(PropertyLogRepository.class);
        OwnerLogService ownerLogService = mock(OwnerLogService.class);
        MailService mailService = mock(MailService.class);
        JwtDecoder tenantJwtDecoder = mock(JwtDecoder.class);
        AdemeApiService ademeApiService = mock(AdemeApiService.class);

        PropertyServiceImpl propertyService = new PropertyServiceImpl(authenticationFacade, propertyRepository, propertyMapper, propertyApartmentSharingService, tenantService, propertyLogRepository, ownerLogService, mailService, ademeApiService);
        ReflectionTestUtils.setField(propertyService, "tenantJwtDecoder", tenantJwtDecoder);

        Owner owner = new Owner();
        owner.setId(1L);
        when(authenticationFacade.getOwner()).thenReturn(owner);

        Property existingProperty = new Property();
        existingProperty.setId(1L);
        existingProperty.setAdemeNumber("2137E1234567A");

        PropertyForm propertyForm = new PropertyForm();
        propertyForm.setId(1L);
        propertyForm.setAdemeNumber("2337E0363555K");

        AdemeResultModel ademeResult = AdemeResultModel.builder()
                .numero("2337E0363555K")
                .consommation("150.0")
                .emission("30.0")
                .dateRealisation("2023-05-12T00:00:00Z")
                .build();
        when(ademeApiService.getDpeDetails("2337E0363555K")).thenReturn(ademeResult);

        when(propertyRepository.findByIdAndOwnerId(1L, 1L)).thenReturn(Optional.of(existingProperty));
        when(propertyRepository.save(any(Property.class))).thenAnswer(AdditionalAnswers.returnsFirstArg());
        when(propertyMapper.toPropertyModel(any(Property.class))).thenReturn(new PropertyModel());

        // Act
        PropertyModel result = propertyService.createOrUpdate(propertyForm);

        // Assert
        assertNotNull(result);
        verify(ademeApiService).getDpeDetails("2337E0363555K");
        assertEquals("2337E0363555K", existingProperty.getAdemeNumber());
        assertEquals(150, existingProperty.getEnergyConsumption());
        assertEquals(30, existingProperty.getCo2Emission());
    }

    @Test
    @Disabled
    // TODO  HttpClient.newHttpClient() is used in PropertyServiceImpl.createOrUpdate -> should be mocked
    public void test_ademe_number_not_null() throws InterruptedException {
        // Arrange
        AuthenticationFacade authenticationFacade = mock(AuthenticationFacade.class);
        PropertyRepository propertyRepository = mock(PropertyRepository.class);
        OwnerPropertyMapper propertyMapper = new OwnerPropertyMapperImpl();
        PropertyApartmentSharingService propertyApartmentSharingService = mock(PropertyApartmentSharingService.class);
        TenantCommonService tenantService = mock(TenantCommonService.class);
        PropertyLogRepository propertyLogRepository = mock(PropertyLogRepository.class);
        OwnerLogService ownerLogService = mock(OwnerLogService.class);
        MailService mailService = mock(MailService.class);
        JwtDecoder tenantJwtDecoder = mock(JwtDecoder.class);
        AdemeApiService ademeApiService = mock(AdemeApiService.class);

        PropertyServiceImpl propertyService = new PropertyServiceImpl(authenticationFacade, propertyRepository, propertyMapper, propertyApartmentSharingService, tenantService, propertyLogRepository, ownerLogService, mailService, ademeApiService);
        ReflectionTestUtils.setField(propertyService, "tenantJwtDecoder", tenantJwtDecoder);

        Owner owner = new Owner();
        owner.setId(1L);
        when(authenticationFacade.getOwner()).thenReturn(owner);

        PropertyForm propertyForm = new PropertyForm();
        propertyForm.setId(1L);
        propertyForm.setAdemeNumber("2337E0363555K");

        Property existingProperty = new Property();
        existingProperty.setId(1L);
        when(propertyRepository.findByIdAndOwnerId(1L, 1L)).thenReturn(Optional.of(existingProperty));

        when(propertyRepository.save(any(Property.class))).thenAnswer(AdditionalAnswers.returnsFirstArg());

        // Act
        PropertyModel result = null;
        result = propertyService.createOrUpdate(propertyForm);

        // Assert
        assertNotNull(result);
        assertEquals("2337E0363555K", result.getAdemeNumber());
    }

}