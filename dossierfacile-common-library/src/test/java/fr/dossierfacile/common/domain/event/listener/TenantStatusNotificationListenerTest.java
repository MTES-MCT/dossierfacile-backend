package fr.dossierfacile.common.domain.event.listener;

import fr.dossierfacile.common.domain.event.TenantStatusChangedEvent;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.User;
import fr.dossierfacile.common.enums.PartnerCallBackType;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.MessageCommonService;
import fr.dossierfacile.common.service.interfaces.PartnerCallBackService;
import fr.dossierfacile.common.service.interfaces.TenantStatusNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantStatusNotificationListenerTest {

    private TenantStatusNotificationListener listener;

    @Mock
    private TenantCommonRepository tenantRepository;

    @Mock
    private PartnerCallBackService partnerCallBackService;

    @Mock
    private TenantStatusNotificationService tenantStatusNotificationService;

    @Mock
    private MessageCommonService messageCommonService;

    @BeforeEach
    void setUp() {
        listener = new TenantStatusNotificationListener(
                tenantRepository,
                partnerCallBackService,
                Optional.of(tenantStatusNotificationService),
                Optional.of(messageCommonService)
        );
    }

    @Test
    void should_trigger_notifications_when_status_validated() {
        User operator = mock(User.class);
        Tenant tenant = Tenant.builder().id(1L).status(TenantFileStatus.VALIDATED).build();
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));

        TenantStatusChangedEvent event = new TenantStatusChangedEvent(1L, TenantFileStatus.TO_PROCESS, TenantFileStatus.VALIDATED, operator);

        listener.onTenantStatusChanged(event);

        verify(messageCommonService).markReadAdmin(tenant);
        verify(tenantStatusNotificationService).notifyTenantValidated(tenant);
        verifyNoInteractions(partnerCallBackService);
    }

    @Test
    void should_trigger_notifications_and_callbacks_when_status_declined() {
        User operator = mock(User.class);
        Tenant tenant = Tenant.builder().id(1L).status(TenantFileStatus.DECLINED).build();
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));

        TenantStatusChangedEvent event = new TenantStatusChangedEvent(1L, TenantFileStatus.VALIDATED, TenantFileStatus.DECLINED, operator);

        listener.onTenantStatusChanged(event);

        verify(messageCommonService).markReadAdmin(tenant);
        verify(tenantStatusNotificationService).notifyTenantDeclined(tenant);
        verify(partnerCallBackService).sendCallBack(tenant, PartnerCallBackType.DENIED_ACCOUNT);
    }

    @Test
    void should_handle_empty_optional_services() {
        TenantStatusNotificationListener listenerWithoutOptionalServices = new TenantStatusNotificationListener(
                tenantRepository,
                partnerCallBackService,
                Optional.empty(),
                Optional.empty()
        );

        User operator = mock(User.class);
        Tenant tenant = Tenant.builder().id(1L).status(TenantFileStatus.VALIDATED).build();
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));

        TenantStatusChangedEvent event = new TenantStatusChangedEvent(1L, TenantFileStatus.TO_PROCESS, TenantFileStatus.VALIDATED, operator);

        listenerWithoutOptionalServices.onTenantStatusChanged(event);

        // Should not throw exceptions and shouldn't call any optional services since they are empty
        verifyNoInteractions(messageCommonService);
        verifyNoInteractions(tenantStatusNotificationService);
    }
}
