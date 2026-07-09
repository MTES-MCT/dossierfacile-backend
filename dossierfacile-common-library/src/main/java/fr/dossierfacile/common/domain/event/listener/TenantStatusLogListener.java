package fr.dossierfacile.common.domain.event.listener;

import fr.dossierfacile.common.domain.event.TenantStatusChangedEvent;
import fr.dossierfacile.common.entity.OperatorLog;
import fr.dossierfacile.common.entity.TenantLog;
import fr.dossierfacile.common.enums.ActionOperatorType;
import fr.dossierfacile.common.enums.LogType;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.repository.OperatorLogCommonRepository;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.TenantLogCommonService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class TenantStatusLogListener {
    private final TenantLogCommonService tenantLogCommonService;
    private final OperatorLogCommonRepository operatorLogRepository;
    private final TenantCommonRepository tenantRepository;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onTenantStatusChanged(TenantStatusChangedEvent event) {
        var tenant = tenantRepository.getReferenceById(event.tenantId());

        if (event.newStatus() == TenantFileStatus.VALIDATED) {
            tenantLogCommonService.saveTenantLog(new TenantLog(LogType.ACCOUNT_VALIDATED, tenant.getId(), event.operator() != null ? event.operator().getId() : null));
            if (event.operator() != null) {
                operatorLogRepository.save(new OperatorLog(tenant, event.operator(), tenant.getStatus(), ActionOperatorType.STOP_PROCESS, 1, null));
            }
        } else if (event.newStatus() == TenantFileStatus.DECLINED) {
            tenantLogCommonService.saveTenantLog(new TenantLog(LogType.ACCOUNT_DENIED, tenant.getId(), event.operator() != null ? event.operator().getId() : null, null));
            if (event.operator() != null) {
                operatorLogRepository.save(new OperatorLog(tenant, event.operator(), tenant.getStatus(), ActionOperatorType.STOP_PROCESS, 1, null));
            }
        }
    }
}
