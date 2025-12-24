package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.mapper.PartnerAccessMapper;
import fr.dossierfacile.api.front.model.tenant.PartnerAccessModel;
import fr.dossierfacile.api.front.service.interfaces.KeycloakService;
import fr.dossierfacile.api.front.service.interfaces.MailService;
import fr.dossierfacile.api.front.service.interfaces.PartnerAccessService;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.TenantUserApi;
import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.repository.TenantUserApiRepository;
import fr.dossierfacile.common.service.interfaces.LogService;
import fr.dossierfacile.common.service.interfaces.PartnerCallBackService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import static fr.dossierfacile.common.constants.PartnerConstants.DF_OWNER_NAME;

@Slf4j
@Service
@AllArgsConstructor
public class PartnerAccessServiceImpl implements PartnerAccessService {

    private final TenantUserApiRepository tenantUserApiRepository;
    private final PartnerCallBackService partnerCallBackService;
    private final TenantCommonRepository tenantRepository;
    private final PartnerAccessMapper partnerAccessMapper;
    private final KeycloakService keycloakService;
    private final MailService mailService;
    private final LogService logService;

    @Override
    public List<PartnerAccessModel> getExternalPartners(Tenant tenant) {
        return tenant.getTenantsUserApi().stream()
                .filter(api -> !DF_OWNER_NAME.equals(api.getUserApi().getName()))
                .map(partnerAccessMapper::toModel)
                .toList();
    }

    @Override
    public void deleteAccess(Tenant tenant, Long userApiId) {
        tenantUserApiRepository.findAllByApartmentSharingAndUserApi(tenant.getApartmentSharing().getId(), userApiId)
                .forEach(this::deleteAccess);
    }

    private void deleteAccess(TenantUserApi tenantUserApi) {
        UserApi userApi = tenantUserApi.getUserApi();
        Tenant tenant = tenantUserApi.getTenant();
        tenant.setLastUpdateDate(LocalDateTime.now());
        tenantRepository.save(tenant);

        tenantUserApiRepository.delete(tenantUserApi);
        partnerCallBackService.sendRevokedAccessCallback(tenant, userApi);
        keycloakService.revokeUserConsent(tenant, userApi);
        logService.savePartnerAccessRevocationLog(tenant, userApi);
        mailService.sendEmailPartnerAccessRevoked(tenant, userApi, tenant);

        log.info("Revoked access of partner {} to tenant {}", userApi.getId(), tenant.getId());
    }

}
