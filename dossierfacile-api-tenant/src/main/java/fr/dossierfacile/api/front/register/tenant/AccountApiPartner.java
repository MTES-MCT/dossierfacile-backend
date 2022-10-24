package fr.dossierfacile.api.front.register.tenant;

import fr.dossierfacile.api.front.mapper.TenantMapper;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.register.SaveStep;
import fr.dossierfacile.api.front.register.form.partner.AccountPartnerForm;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.MailService;
import fr.dossierfacile.api.front.service.interfaces.SourceService;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.PartnerCallBackService;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class AccountApiPartner implements SaveStep<AccountPartnerForm> {

    private final TenantCommonRepository tenantRepository;
    private final TenantService tenantService;
    private final TenantMapper tenantMapper;
    private final AuthenticationFacade authenticationFacade;
    private final SourceService sourceService;
    private final PartnerCallBackService partnerCallBackService;
    private final MailService mailService;

    @Override
    public TenantModel saveStep(Tenant t, AccountPartnerForm accountForm) {
        String email = accountForm.getEmail().toLowerCase();
        Tenant tenant = findOrCreateTenant(email);
        UserApi userApi = this.sourceService.findOrCreate(authenticationFacade.getKeycloakClientId());
        partnerCallBackService.registerTenant(accountForm.getInternalPartnerId(), tenant, userApi);

        mailService.sendEmailWelcomeForPartnerUser(tenant, userApi);

        tenant.lastUpdateDateProfile(LocalDateTime.now(), null);
        return tenantMapper.toTenantModel(tenantRepository.save(tenant));
    }

    private Tenant findOrCreateTenant(String email) {
        Tenant tenant = tenantRepository.findByEmailAndEnabledFalse(email)
                .orElseGet(() -> tenantService.create(new Tenant(email)));
        tenant.setEnabled(true);
        tenantRepository.save(tenant);
        return tenant;
    }

}
