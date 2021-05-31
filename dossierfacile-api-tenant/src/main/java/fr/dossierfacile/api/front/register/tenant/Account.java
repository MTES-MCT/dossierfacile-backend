package fr.dossierfacile.api.front.register.tenant;

import fr.dossierfacile.api.front.mapper.TenantMapper;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.register.SaveStep;
import fr.dossierfacile.api.front.register.form.tenant.AccountForm;
import fr.dossierfacile.api.front.repository.TenantRepository;
import fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService;
import fr.dossierfacile.api.front.service.interfaces.ConfirmationTokenService;
import fr.dossierfacile.api.front.service.interfaces.MailService;
import fr.dossierfacile.api.front.service.interfaces.PartnerCallBackService;
import fr.dossierfacile.api.front.service.interfaces.SourceService;
import fr.dossierfacile.api.front.service.interfaces.UserRoleService;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.UserApi;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@AllArgsConstructor
public class Account implements SaveStep<AccountForm> {

    private final TenantRepository tenantRepository;
    private final ConfirmationTokenService confirmationTokenService;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final UserRoleService userRoleService;
    private final TenantMapper tenantMapper;
    private final ApartmentSharingService apartmentSharingService;
    private final MailService mailService;
    private final SourceService sourceService;
    private final PartnerCallBackService partnerCallBackService;

    @Override
    @Transactional
    public TenantModel saveStep(Tenant t, AccountForm accountForm) {
        String email = accountForm.getEmail().toLowerCase();
        Tenant tenant = tenantRepository.findByEmailAndEnabledFalse(email).orElse(new Tenant(email));
        tenant.setPassword(bCryptPasswordEncoder.encode(accountForm.getPassword()));
        if (accountForm.getSource() != null && !accountForm.getSource().isBlank()) {
            tenant.setFirstName(accountForm.getFirstName());
            tenant.setLastName(accountForm.getLastName());
            tenantRepository.save(tenant);
            UserApi userApi = this.sourceService.findOrCreate(accountForm.getSource());

            partnerCallBackService.registerTenant(accountForm.getInternalPartnerId(), tenant, userApi);
        }
        tenantRepository.save(tenant);
        mailService.sendEmailConfirmAccount(tenant, confirmationTokenService.createToken(tenant));
        userRoleService.createRole(tenant);
        apartmentSharingService.createApartmentSharing(tenant);
        tenant.lastUpdateDateProfile(LocalDateTime.now(), null);
        return tenantMapper.toTenantModel(tenantRepository.save(tenant));
    }
}
