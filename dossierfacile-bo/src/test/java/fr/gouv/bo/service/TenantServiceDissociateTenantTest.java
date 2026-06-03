package fr.gouv.bo.service;

import fr.dossierfacile.common.dto.mail.TenantDto;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.BOUser;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.TenantLog;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.LogType;
import fr.dossierfacile.common.enums.PartnerCallBackType;
import fr.dossierfacile.common.enums.TenantType;
import fr.dossierfacile.common.mapper.mail.TenantMapperForMail;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.ApartmentSharingLinkService;
import fr.dossierfacile.common.service.interfaces.PartnerCallBackService;
import fr.dossierfacile.common.service.interfaces.TenantLogCommonService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantServiceDissociateTenantTest {

    @Mock
    private TenantCommonRepository tenantRepository;
    @Mock
    private MailService mailService;
    @Mock
    private PartnerCallBackService partnerCallBackService;
    @Mock
    private ApartmentSharingService apartmentSharingService;
    @Mock
    private ApartmentSharingLinkService apartmentSharingLinkService;
    @Mock
    private TenantLogCommonService tenantLogCommonService;
    @Mock
    private TenantMapperForMail tenantMapperForMail;

    private TenantService tenantService;

    @BeforeEach
    void setUp() {
        tenantService = new TenantService(
                tenantRepository,
                mailService,
                partnerCallBackService,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                apartmentSharingService,
                apartmentSharingLinkService,
                null,
                tenantMapperForMail,
                null,
                null,
                tenantLogCommonService,
                null,
                null
        );
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void dissociateTenant_shouldCreateIndependentApplicationAndTriggerSideEffects() {
        BOUser operator = BOUser.builder().id(7L).build();
        Tenant createTenant = Tenant.builder().id(1L).tenantType(TenantType.CREATE).email("create@test.com").build();
        Tenant joinTenant = Tenant.builder().id(2L).tenantType(TenantType.JOIN).email("join@test.com").build();
        ApartmentSharing oldApartmentSharing = ApartmentSharing.builder()
                .id(10L)
                .applicationType(ApplicationType.COUPLE)
                .tenants(new ArrayList<>(List.of(createTenant, joinTenant)))
                .build();
        createTenant.setApartmentSharing(oldApartmentSharing);
        joinTenant.setApartmentSharing(oldApartmentSharing);

        TenantDto dissociatedTenantDto = new TenantDto();
        dissociatedTenantDto.setId(2L);
        dissociatedTenantDto.setEmail("join@test.com");
        dissociatedTenantDto.setFirstName("Join");
        dissociatedTenantDto.setLastName("User");
        when(tenantMapperForMail.toDto(joinTenant)).thenReturn(dissociatedTenantDto);
        when(apartmentSharingService.createApartmentSharingFor(joinTenant)).thenAnswer(invocation -> {
            Tenant tenant = invocation.getArgument(0);
            ApartmentSharing apartmentSharing = new ApartmentSharing(tenant);
            apartmentSharing.setId(99L);
            tenant.setApartmentSharing(apartmentSharing);
            return apartmentSharing;
        });

        TransactionSynchronizationManager.initSynchronization();
        Long newApartmentSharingId = tenantService.dissociateTenant(joinTenant, operator);
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        assertThat(newApartmentSharingId).isEqualTo(99L);
        assertThat(joinTenant.getTenantType()).isEqualTo(TenantType.CREATE);
        assertThat(joinTenant.getApartmentSharing().getId()).isEqualTo(99L);

        verify(apartmentSharingService).createApartmentSharingFor(joinTenant);
        verify(apartmentSharingService).removeTenant(oldApartmentSharing, joinTenant);
        verify(apartmentSharingLinkService).revokeAllPartnerAccessForTenant(joinTenant);
        verify(tenantRepository).save(joinTenant);
        verify(partnerCallBackService).sendCallBack(joinTenant, PartnerCallBackType.DISSOCIATED_ACCOUNT);
        verify(partnerCallBackService).sendCallBack(List.of(createTenant), PartnerCallBackType.DISSOCIATED_ACCOUNT);
        verify(mailService).sendEmailTenantDissociated(dissociatedTenantDto);

        ArgumentCaptor<TenantLog> logCaptor = ArgumentCaptor.forClass(TenantLog.class);
        verify(tenantLogCommonService).saveTenantLog(logCaptor.capture());
        assertThat(logCaptor.getValue().getLogType()).isEqualTo(LogType.ACCOUNT_DISSOCIATED);
        assertThat(logCaptor.getValue().getTenantId()).isEqualTo(joinTenant.getId());
        assertThat(logCaptor.getValue().getOperatorId()).isEqualTo(operator.getId());
    }

    @Test
    void dissociateTenant_shouldRejectTenantWithoutEmail() {
        BOUser operator = BOUser.builder().id(7L).build();
        Tenant joinTenant = Tenant.builder().id(2L).tenantType(TenantType.JOIN).email(null).build();
        ApartmentSharing oldApartmentSharing = ApartmentSharing.builder()
                .id(10L)
                .applicationType(ApplicationType.COUPLE)
                .tenants(new ArrayList<>(List.of(joinTenant)))
                .build();
        joinTenant.setApartmentSharing(oldApartmentSharing);

        assertThatThrownBy(() -> tenantService.dissociateTenant(joinTenant, operator))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must have an email");

        verify(apartmentSharingService, never()).createApartmentSharingFor(any());
        verify(partnerCallBackService, never()).sendCallBack(any(Tenant.class), any(PartnerCallBackType.class));
    }
}
