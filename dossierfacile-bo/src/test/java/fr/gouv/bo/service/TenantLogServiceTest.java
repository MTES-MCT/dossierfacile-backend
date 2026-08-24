package fr.gouv.bo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.TenantLog;
import fr.dossierfacile.common.enums.LogType;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.TenantLogCommonService;
import fr.gouv.bo.repository.BoTenantLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TenantLogServiceTest {

    private BoTenantLogRepository logRepository;
    private TenantCommonRepository tenantRepository;
    private TenantLogCommonService tenantLogCommonService;
    private ObjectMapper objectMapper;
    private TenantLogService tenantLogService;

    @BeforeEach
    void setUp() {
        logRepository = mock(BoTenantLogRepository.class);
        tenantRepository = mock(TenantCommonRepository.class);
        tenantLogCommonService = mock(TenantLogCommonService.class);
        objectMapper = mock(ObjectMapper.class);
        tenantLogService = new TenantLogService(logRepository, tenantRepository, tenantLogCommonService, objectMapper);
    }

    @Test
    void isTenantAutoValidated_returns_true_when_tenant_is_validated_and_last_log_is_auto_validated() {
        Long tenantId = 1L;
        Tenant tenant = Tenant.builder().id(tenantId).status(TenantFileStatus.VALIDATED).build();
        when(tenantRepository.findOneById(tenantId)).thenReturn(tenant);

        TenantLog log = TenantLog.builder()
                .logType(LogType.ACCOUNT_AUTOMATICALLY_VALIDATED)
                .creationDateTime(LocalDateTime.now())
                .build();
        when(logRepository.findLogsByTenantId(tenantId)).thenReturn(List.of(log));

        assertThat(tenantLogService.isTenantAutoValidated(tenantId)).isTrue();
        assertThat(tenantLogService.isTenantAutoValidated(tenant)).isTrue();
    }

    @Test
    void isTenantAutoValidated_returns_false_when_tenant_is_to_process_even_with_auto_validated_log() {
        Long tenantId = 1L;
        Tenant tenant = Tenant.builder().id(tenantId).status(TenantFileStatus.TO_PROCESS).build();
        when(tenantRepository.findOneById(tenantId)).thenReturn(tenant);

        TenantLog log = TenantLog.builder()
                .logType(LogType.ACCOUNT_AUTOMATICALLY_VALIDATED)
                .creationDateTime(LocalDateTime.now())
                .build();
        when(logRepository.findLogsByTenantId(tenantId)).thenReturn(List.of(log));

        assertThat(tenantLogService.isTenantAutoValidated(tenantId)).isFalse();
        assertThat(tenantLogService.isTenantAutoValidated(tenant)).isFalse();
    }

    @Test
    void isTenantAutoValidated_returns_false_when_tenant_is_validated_and_last_log_is_manual_validation() {
        Long tenantId = 1L;
        Tenant tenant = Tenant.builder().id(tenantId).status(TenantFileStatus.VALIDATED).build();
        when(tenantRepository.findOneById(tenantId)).thenReturn(tenant);

        TenantLog autoLog = TenantLog.builder()
                .logType(LogType.ACCOUNT_AUTOMATICALLY_VALIDATED)
                .creationDateTime(LocalDateTime.now().minusDays(1))
                .build();
        TenantLog manualLog = TenantLog.builder()
                .logType(LogType.ACCOUNT_VALIDATED)
                .operatorId(42L)
                .creationDateTime(LocalDateTime.now())
                .build();
        when(logRepository.findLogsByTenantId(tenantId)).thenReturn(List.of(autoLog, manualLog));

        assertThat(tenantLogService.isTenantAutoValidated(tenantId)).isFalse();
        assertThat(tenantLogService.isTenantAutoValidated(tenant)).isFalse();
    }

    @Test
    void isTenantAutoValidated_returns_false_when_tenant_is_null() {
        Long tenantId = 1L;
        when(tenantRepository.findOneById(tenantId)).thenReturn(null);

        assertThat(tenantLogService.isTenantAutoValidated(tenantId)).isFalse();
        assertThat(tenantLogService.isTenantAutoValidated((Tenant) null)).isFalse();
    }
}
