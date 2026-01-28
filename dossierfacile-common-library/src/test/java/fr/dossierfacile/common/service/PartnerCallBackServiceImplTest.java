package fr.dossierfacile.common.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.enums.ApartmentSharingLinkType;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.mapper.ApplicationFullMapper;
import fr.dossierfacile.common.repository.ApartmentSharingLinkRepository;
import fr.dossierfacile.common.repository.ApartmentSharingRepository;
import fr.dossierfacile.common.repository.CallbackLogRepository;
import fr.dossierfacile.common.repository.TenantUserApiRepository;
import fr.dossierfacile.common.service.interfaces.RequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PartnerCallBackServiceImplTest {

    private TenantUserApiRepository tenantUserApiRepository;
    private ApplicationFullMapper applicationFullMapper;
    private RequestService requestService;
    private CallbackLogRepository callbackLogRepository;
    private ApartmentSharingRepository apartmentSharingRepository;
    private ApartmentSharingLinkRepository apartmentSharingLinkRepository;
    private ObjectMapper objectMapper;

    private PartnerCallBackServiceImpl service;

    private Tenant tenant;
    private UserApi userApi;
    private ApartmentSharing apartmentSharing;

    @BeforeEach
    void setUp() {
        tenantUserApiRepository = mock(TenantUserApiRepository.class);
        applicationFullMapper = mock(ApplicationFullMapper.class);
        requestService = mock(RequestService.class);
        callbackLogRepository = mock(CallbackLogRepository.class);
        apartmentSharingRepository = mock(ApartmentSharingRepository.class);
        apartmentSharingLinkRepository = mock(ApartmentSharingLinkRepository.class);
        objectMapper = new ObjectMapper();

        service = new PartnerCallBackServiceImpl(
                tenantUserApiRepository,
                applicationFullMapper,
                requestService,
                callbackLogRepository,
                apartmentSharingRepository,
                apartmentSharingLinkRepository,
                objectMapper
        );

        apartmentSharing = ApartmentSharing.builder()
                .id(1L)
                .apartmentSharingLinks(new ArrayList<>())
                .build();

        tenant = Tenant.builder()
                .id(100L)
                .apartmentSharing(apartmentSharing)
                .status(TenantFileStatus.VALIDATED)
                .build();

        userApi = UserApi.builder()
                .id(200L)
                .name("TestPartner")
                .name2("Test Partner")
                .build();
    }

    @Test
    void should_create_partner_links_when_they_do_not_exist() {
        // Given
        when(tenantUserApiRepository.findFirstByTenantAndUserApi(tenant, userApi))
                .thenReturn(Optional.empty());

        when(apartmentSharingLinkRepository.findByApartmentSharingAndPartnerIdAndLinkTypeAndDeletedIsFalse(
                apartmentSharing,
                userApi.getId(),
                ApartmentSharingLinkType.PARTNER
        )).thenReturn(Collections.emptyList());

        // When
        service.registerTenant(tenant, userApi);

        // Then
        verify(tenantUserApiRepository).save(any(TenantUserApi.class));
        verify(apartmentSharingLinkRepository, times(2)).save(any(ApartmentSharingLink.class));
        verify(apartmentSharingLinkRepository).findByApartmentSharingAndPartnerIdAndLinkTypeAndDeletedIsFalse(
                apartmentSharing,
                userApi.getId(),
                ApartmentSharingLinkType.PARTNER
        );
    }

    @Test
    void should_not_recreate_partner_links_when_they_already_exist() {
        // Given
        ApartmentSharingLink existingLink1 = ApartmentSharingLink.builder()
                .id(1L)
                .apartmentSharing(apartmentSharing)
                .partnerId(userApi.getId())
                .linkType(ApartmentSharingLinkType.PARTNER)
                .fullData(false)
                .deleted(false)
                .build();

        ApartmentSharingLink existingLink2 = ApartmentSharingLink.builder()
                .id(2L)
                .apartmentSharing(apartmentSharing)
                .partnerId(userApi.getId())
                .linkType(ApartmentSharingLinkType.PARTNER)
                .fullData(true)
                .deleted(false)
                .build();

        when(tenantUserApiRepository.findFirstByTenantAndUserApi(tenant, userApi))
                .thenReturn(Optional.empty());

        when(apartmentSharingLinkRepository.findByApartmentSharingAndPartnerIdAndLinkTypeAndDeletedIsFalse(
                apartmentSharing,
                userApi.getId(),
                ApartmentSharingLinkType.PARTNER
        )).thenReturn(List.of(existingLink1, existingLink2));

        // When
        service.registerTenant(tenant, userApi);

        // Then
        verify(tenantUserApiRepository).save(any(TenantUserApi.class));
        verify(apartmentSharingLinkRepository, never()).save(any(ApartmentSharingLink.class));
        verify(apartmentSharingLinkRepository).findByApartmentSharingAndPartnerIdAndLinkTypeAndDeletedIsFalse(
                apartmentSharing,
                userApi.getId(),
                ApartmentSharingLinkType.PARTNER
        );
    }


    @Test
    void should_create_links_when_existing_links_are_deleted() {
        // Given - Existing links are marked as deleted
        when(tenantUserApiRepository.findFirstByTenantAndUserApi(tenant, userApi))
                .thenReturn(Optional.empty());

        // Repository should not return deleted links
        when(apartmentSharingLinkRepository.findByApartmentSharingAndPartnerIdAndLinkTypeAndDeletedIsFalse(
                apartmentSharing,
                userApi.getId(),
                ApartmentSharingLinkType.PARTNER
        )).thenReturn(Collections.emptyList());

        // When
        service.registerTenant(tenant, userApi);

        // Then - Should create new links
        verify(tenantUserApiRepository).save(any(TenantUserApi.class));
        verify(apartmentSharingLinkRepository, times(2)).save(any(ApartmentSharingLink.class));
    }
}
