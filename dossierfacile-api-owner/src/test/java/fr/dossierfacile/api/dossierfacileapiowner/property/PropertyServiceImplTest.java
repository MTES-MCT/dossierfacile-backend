package fr.dossierfacile.api.dossierfacileapiowner.property;

import fr.dossierfacile.api.dossierfacileapiowner.log.OwnerLogService;
import fr.dossierfacile.api.dossierfacileapiowner.mail.MailService;
import fr.dossierfacile.api.dossierfacileapiowner.register.AuthenticationFacade;
import fr.dossierfacile.common.entity.Owner;
import fr.dossierfacile.common.entity.Property;
import fr.dossierfacile.common.enums.OwnerLogType;
import fr.dossierfacile.common.repository.PropertyLogRepository;
import fr.dossierfacile.common.service.interfaces.AdemeApiService;
import fr.dossierfacile.common.service.interfaces.TenantCommonService;
import fr.dossierfacile.common.model.AdemeResultModel;
import fr.dossierfacile.common.exceptions.AdemeApiBadRequestException;
import fr.dossierfacile.common.exceptions.AdemeApiNotFoundException;
import fr.dossierfacile.api.dossierfacileapiowner.exception.OwnerApiException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.AdditionalAnswers;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    public void test_does_not_call_ademe_api_when_ademe_number_unchanged() throws Exception {
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
        propertyForm.setRentCost(850.0);
        propertyForm.setAdemeNumber("2178V1001934U");

        Property existingProperty = new Property();
        existingProperty.setId(1L);
        existingProperty.setAdemeNumber("2178V1001934U");
        existingProperty.setRentCost(800.0);
        when(propertyRepository.findByIdAndOwnerId(1L, 1L)).thenReturn(Optional.of(existingProperty));
        when(propertyRepository.save(any(Property.class))).thenAnswer(AdditionalAnswers.returnsFirstArg());
        when(propertyMapper.toPropertyModel(any(Property.class))).thenReturn(new PropertyModel());

        PropertyModel result = propertyService.createOrUpdate(propertyForm);

        assertNotNull(result);
        verify(ademeApiService, never()).getDpeDetails(any());
        verify(propertyRepository).save(argThat(saved -> saved.getRentCost().equals(850.0)));
    }

    @Test
    public void test_calls_ademe_api_when_ademe_number_changes() throws Exception {
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
        propertyForm.setAdemeNumber("2337E0363555K");

        Property existingProperty = new Property();
        existingProperty.setId(1L);
        existingProperty.setAdemeNumber("2178V1001934U");
        when(propertyRepository.findByIdAndOwnerId(1L, 1L)).thenReturn(Optional.of(existingProperty));
        when(propertyRepository.save(any(Property.class))).thenAnswer(AdditionalAnswers.returnsFirstArg());
        when(propertyMapper.toPropertyModel(any(Property.class))).thenReturn(new PropertyModel());

        AdemeResultModel ademeResultModel = new AdemeResultModel();
        ademeResultModel.setNumero("2337E0363555K");
        ademeResultModel.setConsommation("120");
        ademeResultModel.setEmission("25");
        ademeResultModel.setDateRealisation("2021-08-01T00:00:00Z");
        when(ademeApiService.getDpeDetails(eq("2337E0363555K"))).thenReturn(ademeResultModel);

        propertyService.createOrUpdate(propertyForm);

        verify(ademeApiService).getDpeDetails("2337E0363555K");
    }

    @Test
    public void test_throws_owner_api_exception_when_ademe_returns_not_found() throws Exception {
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
        propertyForm.setAdemeNumber("2178V1001934U");

        Property existingProperty = new Property();
        existingProperty.setId(1L);
        when(propertyRepository.findByIdAndOwnerId(1L, 1L)).thenReturn(Optional.of(existingProperty));

        when(ademeApiService.getDpeDetails("2178V1001934U"))
                .thenThrow(new AdemeApiBadRequestException("Le DPE 2178V1001934U est introuvable."));

        OwnerApiException exception = assertThrows(OwnerApiException.class, () -> propertyService.createOrUpdate(propertyForm));

        assertEquals("DPE_NOT_FOUND", exception.getCode().name());
        assertEquals("2178V1001934U", exception.getDetails().get("dpeNumber"));
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