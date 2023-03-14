package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService;
import fr.dossierfacile.api.front.service.interfaces.TenantStatusService;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.PartnerCallBackType;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.PartnerCallBackService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class TenantStatusServiceImpl implements TenantStatusService {
    private ApartmentSharingService apartmentSharingService;
    private final PartnerCallBackService partnerCallBackService;
    private final TenantCommonRepository tenantRepository;

    @Override
    public Tenant updateTenantStatus(Tenant tenant) {
        TenantFileStatus previousStatus = tenant.getStatus();
        log.info("Updating status of tenant with ID [" + tenant.getId() + "] to [" + tenant.getStatus() + "]");
        tenant.setStatus(tenant.computeStatus());
        tenant = tenantRepository.save(tenant);

        if (previousStatus != tenant.getStatus()) {
            switch (tenant.getStatus()) {
                case VALIDATED -> partnerCallBackService.sendCallBack(tenant, PartnerCallBackType.VERIFIED_ACCOUNT);
                case DECLINED -> partnerCallBackService.sendCallBack(tenant, PartnerCallBackType.DENIED_ACCOUNT);
                case TO_PROCESS -> {
                    if (previousStatus == TenantFileStatus.INCOMPLETE) {
                        partnerCallBackService.sendCallBack(tenant, PartnerCallBackType.CREATED_ACCOUNT);
                    }
                }

            }
            apartmentSharingService.refreshUpdateDate(tenant.getApartmentSharing());
        }
        return tenant;
    }

}
