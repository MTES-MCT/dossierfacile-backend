package fr.dossierfacile.common.domain.event.listener;

import fr.dossierfacile.common.domain.event.TenantStatusChangedEvent;
import fr.dossierfacile.common.entity.OperatorLog;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.TenantLog;
import fr.dossierfacile.common.entity.User;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.repository.OperatorLogCommonRepository;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.TenantLogCommonService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantStatusLogListenerTest {

    private TenantStatusLogListener listener;

    @Mock
    private TenantLogCommonService tenantLogCommonService;

    @Mock
    private OperatorLogCommonRepository operatorLogRepository;

    @Mock
    private TenantCommonRepository tenantRepository;

    @BeforeEach
    void setUp() {
        listener = new TenantStatusLogListener(tenantLogCommonService, operatorLogRepository, tenantRepository);
    }

    @Test
    void should_save_logs_when_status_validated() {
        User operator = mock(User.class);
        when(operator.getId()).thenReturn(2L);
        Tenant tenant = Tenant.builder().id(1L).status(TenantFileStatus.VALIDATED).build();
        when(tenantRepository.getReferenceById(1L)).thenReturn(tenant);

        TenantStatusChangedEvent event = new TenantStatusChangedEvent(1L, TenantFileStatus.TO_PROCESS, TenantFileStatus.VALIDATED, operator);

        listener.onTenantStatusChanged(event);

        verify(tenantLogCommonService).saveTenantLog(any(TenantLog.class));
        ArgumentCaptor<OperatorLog> operatorLogCaptor = ArgumentCaptor.forClass(OperatorLog.class);
        verify(operatorLogRepository).save(operatorLogCaptor.capture());
        
        OperatorLog savedLog = operatorLogCaptor.getValue();
        assertThat(savedLog.getTenant().getId()).isEqualTo(1L);
        assertThat(savedLog.getOperator().getId()).isEqualTo(2L);
    }

    @Test
    void should_save_logs_when_status_declined() {
        User operator = mock(User.class);
        Tenant tenant = Tenant.builder().id(1L).status(TenantFileStatus.DECLINED).build();
        when(tenantRepository.getReferenceById(1L)).thenReturn(tenant);

        TenantStatusChangedEvent event = new TenantStatusChangedEvent(1L, TenantFileStatus.VALIDATED, TenantFileStatus.DECLINED, operator);

        listener.onTenantStatusChanged(event);

        verify(tenantLogCommonService).saveTenantLog(any(TenantLog.class));
        verify(operatorLogRepository).save(any(OperatorLog.class));
    }

    @Test
    void should_not_save_operator_log_when_operator_is_null() {
        Tenant tenant = Tenant.builder().id(1L).status(TenantFileStatus.VALIDATED).build();
        when(tenantRepository.getReferenceById(1L)).thenReturn(tenant);

        TenantStatusChangedEvent event = new TenantStatusChangedEvent(1L, TenantFileStatus.TO_PROCESS, TenantFileStatus.VALIDATED, null);

        listener.onTenantStatusChanged(event);

        verify(tenantLogCommonService).saveTenantLog(any(TenantLog.class));
        verifyNoInteractions(operatorLogRepository);
    }
}
