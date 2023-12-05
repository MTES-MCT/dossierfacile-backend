package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.mapper.PartnerAccessMapper;
import fr.dossierfacile.api.front.model.tenant.PartnerAccessModel;
import fr.dossierfacile.api.front.service.interfaces.KeycloakService;
import fr.dossierfacile.api.front.service.interfaces.PartnerAccessService;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.TenantUserApi;
import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.repository.TenantUserApiRepository;
import fr.dossierfacile.common.service.interfaces.PartnerCallBackService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class PartnerAccessServiceImpl implements PartnerAccessService {

    private final TenantUserApiRepository tenantUserApiRepository;
    private final PartnerCallBackService partnerCallBackService;
    private final PartnerAccessMapper partnerAccessMapper;
    private final KeycloakService keycloakService;

    @Override
    public List<PartnerAccessModel> getAllPartners(Tenant tenant) {
        return partnerAccessMapper.toModel(tenant.getTenantsUserApi());
    }

    @Override
    public void deleteAccess(ApartmentSharing apartmentSharing, Long userApiId) {
        tenantUserApiRepository.findAllByApartmentSharingAndUserApi(apartmentSharing.getId(), userApiId)
                .forEach(this::deleteAccess);
    }

    private void deleteAccess(TenantUserApi tenantUserApi) {
        Tenant tenant = tenantUserApi.getTenant();
        UserApi userApi = tenantUserApi.getUserApi();

        tenantUserApiRepository.delete(tenantUserApi);
        partnerCallBackService.sendRevokedAccessCallback(tenant, userApi);
        keycloakService.revokeUserConsent(tenant, userApi);
    }

}
