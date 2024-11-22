package fr.dossierfacile.api.dossierfacileapiowner.property;

import static org.junit.jupiter.api.Assertions.*;

import fr.dossierfacile.api.dossierfacileapiowner.log.OwnerLogService;
import fr.dossierfacile.api.dossierfacileapiowner.mail.MailService;
import fr.dossierfacile.api.dossierfacileapiowner.register.AuthenticationFacade;
import fr.dossierfacile.common.entity.Owner;
import fr.dossierfacile.common.entity.Property;
import fr.dossierfacile.common.enums.OwnerLogType;
import fr.dossierfacile.common.repository.PropertyLogRepository;
import fr.dossierfacile.common.service.interfaces.TenantCommonService;
import org.apache.http.client.HttpResponseException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.AdditionalAnswers;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


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

        PropertyServiceImpl propertyService = new PropertyServiceImpl(authenticationFacade, propertyRepository, propertyMapper, propertyApartmentSharingService, tenantService, propertyLogRepository, ownerLogService, mailService);
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
        try {
            result = propertyService.createOrUpdate(propertyForm);
        } catch (HttpResponseException e) {
            throw new RuntimeException(e);
        }

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

        PropertyServiceImpl propertyService = new PropertyServiceImpl(authenticationFacade, propertyRepository, propertyMapper, propertyApartmentSharingService, tenantService, propertyLogRepository, ownerLogService, mailService);
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
        try {
            result = propertyService.createOrUpdate(propertyForm);
        } catch (HttpResponseException e) {
            throw new RuntimeException(e);
        }

        // Assert
        assertNotNull(result);
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

        PropertyServiceImpl propertyService = new PropertyServiceImpl(authenticationFacade, propertyRepository, propertyMapper, propertyApartmentSharingService, tenantService, propertyLogRepository, ownerLogService, mailService);
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
        try {
            result = propertyService.createOrUpdate(propertyForm);
        } catch (HttpResponseException e) {
            throw new RuntimeException(e);
        }

        // Assert
        assertNotNull(result);
        assertEquals("2337E0363555K", result.getAdemeNumber());
    }

}