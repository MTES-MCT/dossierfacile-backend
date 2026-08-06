package fr.dossierfacile.common.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.dossierfacile.common.dto.mail.TenantDto;
import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.enums.ApartmentSharingLinkType;
import fr.dossierfacile.common.enums.LogType;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.mapper.ApplicationFullMapper;
import fr.dossierfacile.common.mapper.mail.TenantMapperForMail;
import fr.dossierfacile.common.repository.ApartmentSharingLinkRepository;
import fr.dossierfacile.common.repository.ApartmentSharingRepository;
import fr.dossierfacile.common.repository.CallbackLogRepository;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.repository.TenantUserApiRepository;
import fr.dossierfacile.common.service.interfaces.MailCommonService;
import fr.dossierfacile.common.service.interfaces.RequestService;
import fr.dossierfacile.common.service.interfaces.TenantLogCommonService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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
    private TenantCommonRepository tenantCommonRepository;
    private TenantLogCommonService tenantLogCommonService;
    private TenantMapperForMail tenantMapperForMail;
    private MailCommonService mailCommonService;

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
        tenantCommonRepository = mock(TenantCommonRepository.class);
        tenantLogCommonService = mock(TenantLogCommonService.class);
        tenantMapperForMail = mock(TenantMapperForMail.class);
        mailCommonService = mock(MailCommonService.class);

        service = new PartnerCallBackServiceImpl(
                tenantUserApiRepository,
                applicationFullMapper,
                requestService,
                callbackLogRepository,
                apartmentSharingRepository,
                apartmentSharingLinkRepository,
                objectMapper,
                tenantCommonRepository,
                tenantLogCommonService,
                tenantMapperForMail,
                Optional.of(mailCommonService)
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

    @Test
    void should_switch_completed_dossier_back_to_processing_when_linked_to_partner() {
        // Given - a COMPLETED dossier must never be exposed to partners
        tenant.setStatus(TenantFileStatus.COMPLETED);
        tenant.setValidationRequested(null);
        TenantDto tenantDto = mock(TenantDto.class);
        when(tenantMapperForMail.toDto(tenant)).thenReturn(tenantDto);
        when(tenantUserApiRepository.findFirstByTenantAndUserApi(tenant, userApi))
                .thenReturn(Optional.empty());
        when(apartmentSharingLinkRepository.findByApartmentSharingAndPartnerIdAndLinkTypeAndDeletedIsFalse(
                apartmentSharing, userApi.getId(), ApartmentSharingLinkType.PARTNER
        )).thenReturn(Collections.emptyList());
        TransactionSynchronizationManager.initSynchronization();

        try {
            // When
            service.registerTenant(tenant, userApi);

            // Then - the dossier goes back to the operator queue, positioned at switch time
            assertThat(tenant.getStatus()).isEqualTo(TenantFileStatus.TO_PROCESS);
            assertThat(tenant.getLastUpdateDate()).isNotNull();
            // The user's explicit choice is left untouched
            assertThat(tenant.getValidationRequested()).isNull();
            verify(tenantCommonRepository).save(tenant);
            verify(tenantLogCommonService).saveTenantLog(argThat(log ->
                    log.getLogType() == LogType.COMPLETED_SWITCHED_TO_PROCESS && log.getTenantId().equals(tenant.getId())));

            // And - the mail is sent after commit
            verify(mailCommonService, never()).sendEmailCompletedSwitchedToProcessing(any());
            TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
            verify(mailCommonService).sendEmailCompletedSwitchedToProcessing(tenantDto);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void should_not_switch_status_when_tenant_is_not_completed() {
        // Given - tenant is VALIDATED (from setUp)
        when(tenantUserApiRepository.findFirstByTenantAndUserApi(tenant, userApi))
                .thenReturn(Optional.empty());
        when(apartmentSharingLinkRepository.findByApartmentSharingAndPartnerIdAndLinkTypeAndDeletedIsFalse(
                apartmentSharing, userApi.getId(), ApartmentSharingLinkType.PARTNER
        )).thenReturn(Collections.emptyList());

        // When
        service.registerTenant(tenant, userApi);

        // Then
        assertThat(tenant.getStatus()).isEqualTo(TenantFileStatus.VALIDATED);
        verify(tenantCommonRepository, never()).save(any(Tenant.class));
        verify(tenantLogCommonService, never()).saveTenantLog(any(TenantLog.class));
        verify(mailCommonService, never()).sendEmailCompletedSwitchedToProcessing(any());
    }
}
