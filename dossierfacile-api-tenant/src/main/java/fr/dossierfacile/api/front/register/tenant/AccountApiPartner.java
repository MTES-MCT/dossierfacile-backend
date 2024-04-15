package fr.dossierfacile.api.front.register.tenant;

import fr.dossierfacile.api.front.mapper.TenantMapper;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.register.SaveStep;
import fr.dossierfacile.api.front.register.form.partner.AccountPartnerForm;
import fr.dossierfacile.api.front.security.interfaces.ClientAuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.MailService;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.enums.TenantType;
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
    private final TenantService tenantService;
    private final TenantMapper tenantMapper;
    private final ClientAuthenticationFacade clientAuthenticationFacade;
    private final PartnerCallBackService partnerCallBackService;
    private final MailService mailService;

    @Override
    public TenantModel saveStep(Tenant t, AccountPartnerForm accountForm) {
        String email = accountForm.getEmail().toLowerCase();
        Tenant tenant = findDisabledTenantOrCreateTenant(email);
        UserApi userApi = clientAuthenticationFacade.getClient();
        partnerCallBackService.registerTenant(accountForm.getInternalPartnerId(), tenant, userApi);

        mailService.sendEmailWelcomeForPartnerUser(tenant, userApi);

        tenant.lastUpdateDateProfile(LocalDateTime.now(), null);
        return tenantMapper.toTenantModel(tenantRepository.save(tenant));
    }

    private Tenant findDisabledTenantOrCreateTenant(String email) {
        Tenant tenant = tenantRepository.findByEmailAndEnabledFalse(email)
                .orElseGet(() -> tenantService.create(Tenant.builder().email(email).tenantType(TenantType.CREATE).build()));
        tenant.setEnabled(true);
        tenantRepository.save(tenant);
        return tenant;
    }

}
