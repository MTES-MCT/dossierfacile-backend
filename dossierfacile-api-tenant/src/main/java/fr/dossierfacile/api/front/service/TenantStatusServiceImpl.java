package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService;
import fr.dossierfacile.api.front.service.interfaces.TenantStatusService;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.TenantLog;
import fr.dossierfacile.common.enums.LogType;
import fr.dossierfacile.common.enums.PartnerCallBackType;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.PartnerCallBackService;
import fr.dossierfacile.common.service.interfaces.TenantCommonService;
import fr.dossierfacile.common.service.interfaces.TenantLogCommonService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@AllArgsConstructor
public class TenantStatusServiceImpl implements TenantStatusService {
    private ApartmentSharingService apartmentSharingService;
    private final PartnerCallBackService partnerCallBackService;
    private final TenantCommonRepository tenantRepository;
    private final TenantCommonService tenantCommonService;
    private final TenantLogCommonService tenantLogCommonService;

    @Override
    @Transactional(propagation = Propagation.SUPPORTS, isolation = Isolation.READ_COMMITTED)
    public Tenant updateTenantStatus(Tenant tenant) {
        TenantFileStatus previousStatus = tenant.getStatus();
        log.info("Updating status of tenant with ID [" + tenant.getId() + "] to [" + tenant.getStatus() + "]");
        // load guarantor to the tx context before computeStatus() call
        if (tenant.getGuarantors() != null) {
            tenant.getGuarantors().forEach(Guarantor::getDocuments);
        }

        var newTenantStatus = tenant.computeStatus();
        if (previousStatus != newTenantStatus) {
            if (newTenantStatus == TenantFileStatus.VALIDATED) {
                tenantCommonService.changeTenantStatusToValidated(tenant);
                tenantLogCommonService.saveTenantLog(new TenantLog(LogType.ACCOUNT_VALIDATED, tenant.getId()));

            } else {
                tenant.setStatus(newTenantStatus);
                tenant = tenantRepository.save(tenant);
                if (newTenantStatus == TenantFileStatus.DECLINED) {
                    partnerCallBackService.sendCallBack(tenant, PartnerCallBackType.DENIED_ACCOUNT);
                } else if (newTenantStatus == TenantFileStatus.TO_PROCESS && previousStatus == TenantFileStatus.INCOMPLETE) {
                    partnerCallBackService.sendCallBack(tenant, PartnerCallBackType.CREATED_ACCOUNT);
                }
            }
            apartmentSharingService.refreshUpdateDate(tenant.getApartmentSharing());
        }
        return tenant;
    }

}
