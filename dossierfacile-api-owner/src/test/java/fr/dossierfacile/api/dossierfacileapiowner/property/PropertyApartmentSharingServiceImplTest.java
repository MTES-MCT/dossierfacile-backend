package fr.dossierfacile.api.dossierfacileapiowner.property;

import fr.dossierfacile.api.dossierfacileapiowner.mail.MailService;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.ApartmentSharingLink;
import fr.dossierfacile.common.entity.Owner;
import fr.dossierfacile.common.entity.Property;
import fr.dossierfacile.common.entity.PropertyApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.ApartmentSharingLinkType;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.enums.TenantType;
import fr.dossierfacile.common.repository.ApartmentSharingLinkRepository;
import fr.dossierfacile.common.repository.PropertyLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PropertyApartmentSharingServiceImplTest {

    private ApartmentSharingLinkRepository apartmentSharingLinkRepository;
    private PropertyApartmentSharingRepository propertyApartmentSharingRepository;
    private MailService mailService;
    private PropertyApartmentSharingServiceImpl service;

    @BeforeEach
    void setUp() {
        apartmentSharingLinkRepository = mock(ApartmentSharingLinkRepository.class);
        propertyApartmentSharingRepository = mock(PropertyApartmentSharingRepository.class);
        PropertyLogRepository logRepository = mock(PropertyLogRepository.class);
        mailService = mock(MailService.class);
        service = new PropertyApartmentSharingServiceImpl(
                apartmentSharingLinkRepository, propertyApartmentSharingRepository, logRepository, mailService);
    }

    @Test
    void firstSubscription_createsPASAndOwnerLink() {
        Tenant tenant = buildTenant();
        Property property = buildProperty();
        when(propertyApartmentSharingRepository.findByPropertyAndApartmentSharing(property, tenant.getApartmentSharing()))
                .thenReturn(Optional.empty());

        service.subscribeTenantApartmentSharingToProperty(tenant, property, true);

        verify(propertyApartmentSharingRepository).save(any(PropertyApartmentSharing.class));
        verify(apartmentSharingLinkRepository).save(any(ApartmentSharingLink.class));
        verify(mailService).sendEmailNewApplicantValidated(eq(tenant), any(Owner.class), eq(property));
    }

    @Test
    void reSubscribeAfterTenantRevocation_createsNewOwnerLink() {
        Tenant tenant = buildTenant();
        Property property = buildProperty();

        // Soft-deleted OWNER link from previous candidature
        ApartmentSharingLink deletedLink = ApartmentSharingLink.builder()
                .linkType(ApartmentSharingLinkType.OWNER)
                .propertyId(property.getId())
                .deleted(true)
                .expirationDate(LocalDateTime.now().minusDays(1))
                .build();
        tenant.getApartmentSharing().setApartmentSharingLinks(new ArrayList<>(List.of(deletedLink)));

        PropertyApartmentSharing existingPAS = PropertyApartmentSharing.builder()
                .property(property)
                .apartmentSharing(tenant.getApartmentSharing())
                .build();
        when(propertyApartmentSharingRepository.findByPropertyAndApartmentSharing(property, tenant.getApartmentSharing()))
                .thenReturn(Optional.of(existingPAS));

        service.subscribeTenantApartmentSharingToProperty(tenant, property, true);

        // PAS should NOT be created again
        verify(propertyApartmentSharingRepository, never()).save(any());
        // But a new OWNER link should be created
        verify(apartmentSharingLinkRepository).save(any(ApartmentSharingLink.class));
        verify(mailService).sendEmailNewApplicantValidated(eq(tenant), any(Owner.class), eq(property));
    }

    @Test
    void reSubscribeWhenActiveLinkExists_doesNothing() {
        Tenant tenant = buildTenant();
        Property property = buildProperty();

        // Active OWNER link still exists
        ApartmentSharingLink activeLink = ApartmentSharingLink.builder()
                .linkType(ApartmentSharingLinkType.OWNER)
                .propertyId(property.getId())
                .deleted(false)
                .expirationDate(LocalDateTime.now().plusMonths(1))
                .build();
        tenant.getApartmentSharing().setApartmentSharingLinks(new ArrayList<>(List.of(activeLink)));

        PropertyApartmentSharing existingPAS = PropertyApartmentSharing.builder()
                .property(property)
                .apartmentSharing(tenant.getApartmentSharing())
                .build();
        when(propertyApartmentSharingRepository.findByPropertyAndApartmentSharing(property, tenant.getApartmentSharing()))
                .thenReturn(Optional.of(existingPAS));

        service.subscribeTenantApartmentSharingToProperty(tenant, property, true);

        verify(propertyApartmentSharingRepository, never()).save(any());
        verify(apartmentSharingLinkRepository, never()).save(any());
        verifyNoInteractions(mailService);
    }

    @Test
    void nonCreateTenant_throwsIllegalState() {
        Tenant tenant = buildTenant();
        tenant.setTenantType(TenantType.JOIN);
        Property property = buildProperty();

        assertThrows(IllegalStateException.class,
                () -> service.subscribeTenantApartmentSharingToProperty(tenant, property, true));
    }

    private Tenant buildTenant() {
        ApartmentSharing apartmentSharing = ApartmentSharing.builder()
                .id(1L)
                .apartmentSharingLinks(new ArrayList<>())
                .tenants(new ArrayList<>())
                .build();

        Tenant tenant = new Tenant();
        tenant.setId(1L);
        tenant.setTenantType(TenantType.CREATE);
        tenant.setStatus(TenantFileStatus.VALIDATED);
        tenant.setApartmentSharing(apartmentSharing);
        apartmentSharing.getTenants().add(tenant);
        return tenant;
    }

    private Property buildProperty() {
        Owner owner = new Owner();
        owner.setId(1L);

        Property property = new Property();
        property.setId(1L);
        property.setOwner(owner);
        property.setAddress("10 rue de la Paix");
        return property;
    }
}
