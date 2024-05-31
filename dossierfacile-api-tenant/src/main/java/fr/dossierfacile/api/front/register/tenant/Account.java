package fr.dossierfacile.api.front.register.tenant;

import com.google.common.base.Strings;
import fr.dossierfacile.api.front.mapper.TenantMapper;
import fr.dossierfacile.api.front.mapper.mail.TenantMapperForMail;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.register.SaveStep;
import fr.dossierfacile.api.front.register.form.tenant.AccountForm;
import fr.dossierfacile.api.front.service.interfaces.*;
import fr.dossierfacile.api.front.util.Obfuscator;
import fr.dossierfacile.api.front.util.TransactionalUtil;
import fr.dossierfacile.common.entity.ConfirmationToken;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.enums.TenantType;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.ConfirmationTokenService;
import fr.dossierfacile.common.service.interfaces.PartnerCallBackService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@AllArgsConstructor
@Slf4j
public class Account implements SaveStep<AccountForm> {

    private final TenantCommonRepository tenantRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final UserRoleService userRoleService;
    private final TenantMapper tenantMapper;
    private final ApartmentSharingService apartmentSharingService;
    private final TenantService tenantService;
    private final UserApiService userApiService;
    private final PartnerCallBackService partnerCallBackService;
    private final MailService mailService;
    private final ConfirmationTokenService confirmationTokenService;
    private final KeycloakService keycloakService;
    private final TenantMapperForMail tenantMapperForMail;

    @Override
    @Transactional
    public TenantModel saveStep(Tenant t, AccountForm accountForm) {
        String email = accountForm.getEmail().toLowerCase();
        Optional<Tenant> existingEmailTenant = tenantRepository.findByEmailAndEnabledFalse(email);
        if (existingEmailTenant.isPresent()) {
            throw new IllegalStateException("Tenant " + Obfuscator.email(email) + " already exists (same email)");
        }

        Tenant tenant = tenantService.create(Tenant.builder().tenantType(TenantType.CREATE).email(email).build());
        tenant.setPassword(bCryptPasswordEncoder.encode(accountForm.getPassword()));
        String newKeycloakId = keycloakService.createKeycloakUserAccountCreation(accountForm, tenant);

        Tenant existingTenant = tenantRepository.findByKeycloakId(newKeycloakId);
        if (existingTenant != null) {
            // A tenant already exists, should never happen here because we have already checked existing email
            // and there is no FranceConnect
            throw new IllegalStateException("Tenant " + Obfuscator.email(tenant.getEmail()) + " already exists (same keycloak id)");
        }

        tenant.setKeycloakId(newKeycloakId);
        if (!Strings.isNullOrEmpty(accountForm.getSource())) {
            if (!tenant.getFranceConnect()) {
                tenant.setFirstName(accountForm.getFirstName());
                tenant.setLastName(accountForm.getLastName());
            }
            tenant.setPreferredName(accountForm.getPreferredName());
            tenantRepository.save(tenant);
            Optional<UserApi> userApi = userApiService.findByName(accountForm.getSource());
            userApi.ifPresent(api -> partnerCallBackService.registerTenant(accountForm.getInternalPartnerId(), tenant, api));
        }

        tenantRepository.save(tenant);
        userRoleService.createRole(tenant);
        apartmentSharingService.resetDossierPdfGenerated(tenant.getApartmentSharing());
        tenant.lastUpdateDateProfile(LocalDateTime.now(), null);
        ConfirmationToken confirmationToken = confirmationTokenService.createToken(tenant);

        TransactionalUtil.afterCommit(() -> mailService.sendEmailConfirmAccount(tenantMapperForMail.toDto(tenant), confirmationToken));
        return tenantMapper.toTenantModel(tenantRepository.save(tenant));
    }

}
