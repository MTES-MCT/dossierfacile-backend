package fr.dossierfacile.scheduler.tasks.document;

import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.PartnerCallBackType;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.PartnerCallBackService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@AllArgsConstructor
public class PartnerCallbackService {
    private final TenantCommonRepository tenantCommonRepository;
    private final PartnerCallBackService partnerCallBackService;

    @Transactional
    public void sendPartnerCallback(Long tenantId) {
        log.debug("Send Callback to partners");
        Optional<Tenant> tenant = tenantCommonRepository.findById(tenantId);
        if (tenant.isPresent()) {
            PartnerCallBackType partnerCallBackType = tenant.get().getStatus() == TenantFileStatus.VALIDATED ?
                    PartnerCallBackType.VERIFIED_ACCOUNT :
                    PartnerCallBackType.CREATED_ACCOUNT;
            partnerCallBackService.sendCallBack(tenant.get(), partnerCallBackType);
        }
    }
}