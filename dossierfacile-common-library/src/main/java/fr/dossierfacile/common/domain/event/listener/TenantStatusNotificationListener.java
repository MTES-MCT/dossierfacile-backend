package fr.dossierfacile.common.domain.event.listener;

import fr.dossierfacile.common.domain.event.TenantStatusChangedEvent;
import fr.dossierfacile.common.enums.PartnerCallBackType;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.MessageCommonService;
import fr.dossierfacile.common.service.interfaces.PartnerCallBackService;
import fr.dossierfacile.common.service.interfaces.TenantStatusNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class TenantStatusNotificationListener {
    private final TenantCommonRepository tenantRepository;
    private final PartnerCallBackService partnerCallBackService;
    private final Optional<TenantStatusNotificationService> tenantStatusNotificationService;
    private final Optional<MessageCommonService> messageCommonService;

    // S'exécute dans un thread séparé pour ne pas bloquer le traitement principal de l'utilisateur
    @Async
    // S'exécute uniquement si la transaction principale a réussi (commit)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTenantStatusChanged(TenantStatusChangedEvent event) {
        var tenant = tenantRepository.findById(event.tenantId()).orElseThrow();

        if (event.newStatus() == TenantFileStatus.VALIDATED) {
            messageCommonService.ifPresent(service -> service.markReadAdmin(tenant));
            tenantStatusNotificationService.ifPresent(service -> service.notifyTenantValidated(tenant));
        } else if (event.newStatus() == TenantFileStatus.DECLINED) {
            messageCommonService.ifPresent(service -> service.markReadAdmin(tenant));
            partnerCallBackService.sendCallBack(tenant, PartnerCallBackType.DENIED_ACCOUNT);
            tenantStatusNotificationService.ifPresent(service -> service.notifyTenantDeclined(tenant));
        }
    }
}
