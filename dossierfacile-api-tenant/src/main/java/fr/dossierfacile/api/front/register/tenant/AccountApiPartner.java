package fr.dossierfacile.api.front.register.tenant;

import fr.dossierfacile.api.front.mapper.TenantMapper;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.register.SaveStep;
import fr.dossierfacile.api.front.register.form.partner.AccountPartnerForm;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService;
import fr.dossierfacile.api.front.service.interfaces.SourceService;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.PartnerCallBackService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@AllArgsConstructor
@Slf4j
public class AccountApiPartner implements SaveStep<AccountPartnerForm> {

    private final TenantCommonRepository tenantRepository;
    private final ApartmentSharingService apartmentSharingService;
    private final TenantMapper tenantMapper;
    private final AuthenticationFacade authenticationFacade;
    private final SourceService sourceService;
    private final PartnerCallBackService partnerCallBackService;

    @Override
    public TenantModel saveStep(Tenant t, AccountPartnerForm accountForm) {
        String email = accountForm.getEmail().toLowerCase();
//        Tenant tenant = tenantRepository.findByEmailAndEnabledFalse(email).orElse(new Tenant(email));
        //testing if fixed FC problem
        Tenant tenant = tenantRepository.findByEmailAndEnabledFalse(email).orElseGet(() -> {
            ApartmentSharing apartmentSharing = apartmentSharingService.createApartmentSharing();
            Tenant tenant1 = new Tenant(email);
            apartmentSharing.addTenant(tenant1);
            return tenant1;
        });
        tenant.setEnabled(true);
        if (accountForm.getSource() != null && !accountForm.getSource().isBlank()) {
            tenantRepository.save(tenant);
            UserApi userApi = this.sourceService.findOrCreate(accountForm.getSource());
            partnerCallBackService.registerTenant(accountForm.getInternalPartnerId(), tenant, userApi);
        }
        tenant.addLinkedKeycloakClient(authenticationFacade.getKeycloakClientId());
        tenantRepository.save(tenant);
        //testing if fixed FC problem
//        apartmentSharingService.createApartmentSharing(tenant);
        tenant.lastUpdateDateProfile(LocalDateTime.now(), null);
        return tenantMapper.toTenantModel(tenantRepository.save(tenant));
    }
}
