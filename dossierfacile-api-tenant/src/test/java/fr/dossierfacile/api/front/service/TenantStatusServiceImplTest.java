package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.PartnerCallBackType;
import fr.dossierfacile.common.enums.QueueEntrySource;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.LotteryTicketService;
import fr.dossierfacile.common.service.interfaces.OperatorReviewPolicy;
import fr.dossierfacile.common.service.interfaces.PartnerCallBackService;
import fr.dossierfacile.common.service.interfaces.TenantCommonService;
import fr.dossierfacile.common.service.interfaces.TenantLogCommonService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantStatusServiceImplTest {

    @Mock
    private ApartmentSharingService apartmentSharingService;
    @Mock
    private PartnerCallBackService partnerCallBackService;
    @Mock
    private TenantCommonRepository tenantRepository;
    @Mock
    private TenantCommonService tenantCommonService;
    @Mock
    private TenantLogCommonService tenantLogCommonService;
    @Mock
    private OperatorReviewPolicy operatorReviewPolicy;
    @Mock
    private LotteryTicketService lotteryTicketService;

    @InjectMocks
    private TenantStatusServiceImpl service;

    // The computed status is stubbed on a spy; the policy decides the persisted one
    private Tenant tenantGoingFrom(TenantFileStatus previous, TenantFileStatus computed, TenantFileStatus resolved) {
        Tenant tenant = spy(Tenant.builder().id(1L).status(previous).apartmentSharing(new ApartmentSharing()).build());
        doReturn(computed).when(tenant).computeStatus();
        when(operatorReviewPolicy.resolveStatus(tenant, computed)).thenReturn(resolved);
        if (resolved != TenantFileStatus.VALIDATED && resolved != previous) {
            when(tenantRepository.save(tenant)).thenReturn(tenant);
        }
        return tenant;
    }

    @Nested
    class EnteringCompleted {

        @Test
        void submission_emitsCompletedAccount() {
            Tenant tenant = tenantGoingFrom(TenantFileStatus.INCOMPLETE, TenantFileStatus.TO_PROCESS, TenantFileStatus.COMPLETED);

            Tenant updated = service.updateTenantStatus(tenant);

            assertThat(updated.getStatus()).isEqualTo(TenantFileStatus.COMPLETED);
            verify(partnerCallBackService).sendCallBack(tenant, PartnerCallBackType.COMPLETED_ACCOUNT);
            verify(tenantLogCommonService, never()).logQueueEntered(anyLong(), any());
        }

        // For the partner the dossier is unchanged, as when it enters the queue
        @Test
        void withdrawingTheReviewRequest_isSilent() {
            Tenant tenant = tenantGoingFrom(TenantFileStatus.TO_PROCESS, TenantFileStatus.TO_PROCESS, TenantFileStatus.COMPLETED);

            service.updateTenantStatus(tenant);

            verify(partnerCallBackService, never()).sendCallBack(any(Tenant.class), any(PartnerCallBackType.class));
        }
    }

    @Nested
    class EnteringTheQueue {

        @Test
        void submission_emitsCreatedAccount() {
            Tenant tenant = tenantGoingFrom(TenantFileStatus.INCOMPLETE, TenantFileStatus.TO_PROCESS, TenantFileStatus.TO_PROCESS);

            service.updateTenantStatus(tenant);

            verify(tenantLogCommonService).logQueueEntered(1L, QueueEntrySource.SUBMISSION);
            verify(partnerCallBackService).sendCallBack(tenant, PartnerCallBackType.CREATED_ACCOUNT);
        }

        // For the partner the dossier is unchanged: the next event is the operator's verdict
        @Test
        void fromCompleted_entersTheQueueSilently() {
            Tenant tenant = tenantGoingFrom(TenantFileStatus.COMPLETED, TenantFileStatus.TO_PROCESS, TenantFileStatus.TO_PROCESS);

            service.updateTenantStatus(tenant);

            verify(tenantLogCommonService).logQueueEntered(1L, QueueEntrySource.SUBMISSION);
            verify(partnerCallBackService, never()).sendCallBack(any(Tenant.class), any(PartnerCallBackType.class));
        }

        // Historical behaviour, unchanged: no webhook when a reviewed dossier is edited
        @Test
        void fromValidated_entersTheQueueSilently() {
            Tenant tenant = tenantGoingFrom(TenantFileStatus.VALIDATED, TenantFileStatus.TO_PROCESS, TenantFileStatus.TO_PROCESS);

            service.updateTenantStatus(tenant);

            verify(partnerCallBackService, never()).sendCallBack(any(Tenant.class), any(PartnerCallBackType.class));
        }
    }

    @Test
    void declined_emitsDeniedAccount() {
        Tenant tenant = tenantGoingFrom(TenantFileStatus.TO_PROCESS, TenantFileStatus.DECLINED, TenantFileStatus.DECLINED);

        service.updateTenantStatus(tenant);

        verify(lotteryTicketService).consumeDrawnTicket(1L);
        verify(partnerCallBackService).sendCallBack(tenant, PartnerCallBackType.DENIED_ACCOUNT);
    }

    @Test
    void validated_delegatesToTheCommonService() {
        Tenant tenant = tenantGoingFrom(TenantFileStatus.TO_PROCESS, TenantFileStatus.VALIDATED, TenantFileStatus.VALIDATED);

        service.updateTenantStatus(tenant);

        verify(tenantCommonService).changeTenantStatusToValidated(tenant);
        verify(partnerCallBackService, never()).sendCallBack(any(Tenant.class), any(PartnerCallBackType.class));
    }

    @Test
    void unchangedStatus_doesNothing() {
        Tenant tenant = tenantGoingFrom(TenantFileStatus.COMPLETED, TenantFileStatus.TO_PROCESS, TenantFileStatus.COMPLETED);

        service.updateTenantStatus(tenant);

        verify(tenantRepository, never()).save(any());
        verify(partnerCallBackService, never()).sendCallBack(any(Tenant.class), any(PartnerCallBackType.class));
        verify(apartmentSharingService, never()).refreshUpdateDate(any());
    }
}
