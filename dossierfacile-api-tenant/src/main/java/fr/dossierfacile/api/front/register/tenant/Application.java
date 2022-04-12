package fr.dossierfacile.api.front.register.tenant;

import fr.dossierfacile.api.front.mapper.TenantMapper;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.register.SaveStep;
import fr.dossierfacile.api.front.register.form.tenant.ApplicationForm;
import fr.dossierfacile.api.front.repository.ApartmentSharingRepository;
import fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService;
import fr.dossierfacile.api.front.service.interfaces.KeycloakService;
import fr.dossierfacile.api.front.service.interfaces.LogService;
import fr.dossierfacile.api.front.service.interfaces.MailService;
import fr.dossierfacile.api.front.service.interfaces.PasswordRecoveryTokenService;
import fr.dossierfacile.api.front.service.interfaces.UserRoleService;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.PasswordRecoveryToken;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.LogType;
import fr.dossierfacile.common.enums.PartnerCallBackType;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.PartnerCallBackService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class Application implements SaveStep<ApplicationForm> {

    private final TenantCommonRepository tenantRepository;
    private final UserRoleService userRoleService;
    private final TenantMapper tenantMapper;
    private final MailService mailService;
    private final PasswordRecoveryTokenService passwordRecoveryTokenService;
    private final ApartmentSharingRepository apartmentSharingRepository;
    private final LogService logService;
    private final KeycloakService keycloakService;
    private final ApartmentSharingService apartmentSharingService;
    private final PartnerCallBackService partnerCallBackService;

    @Override
    @Transactional
    public TenantModel saveStep(Tenant tenant, ApplicationForm applicationForm) {
        List<String> oldCoTenant = tenantRepository.findAllByApartmentSharing(tenant.getApartmentSharing())
                .stream()
                .filter(t -> !t.getId().equals(tenant.getId()))
                .map(Tenant::getEmail).collect(Collectors.toList());

        List<String> newCoTenant = applicationForm.getCoTenantEmail();

        List<String> tenantToDelete = oldCoTenant.stream()
                .filter(e -> !newCoTenant.contains(e))
                .collect(Collectors.toList());

        List<String> tenantToCreate = newCoTenant.stream()
                .filter(e -> !oldCoTenant.contains(e))
                .collect(Collectors.toList());

        ApartmentSharing apartmentSharing = tenant.getApartmentSharing();
        apartmentSharing.setApplicationType(applicationForm.getApplicationType());
        apartmentSharingRepository.save(apartmentSharing);
        apartmentSharingService.resetDossierPdfGenerated(apartmentSharing);
        var tenantEntityToDelete = tenantRepository.findByListEmail(tenantToDelete);
        partnerCallBackService.sendCallBack(tenantEntityToDelete, PartnerCallBackType.DELETED_ACCOUNT);
        keycloakService.deleteKeycloakUsers(tenantEntityToDelete);
        tenantRepository.deleteAll(tenantEntityToDelete);

        LocalDateTime now = LocalDateTime.now();
        tenant.lastUpdateDateProfile(now, null);
        tenantRepository.save(tenant);

        tenantToCreate.forEach(email -> {
            Tenant joinTenant = new Tenant(email, apartmentSharing);
            joinTenant.lastUpdateDateProfile(now, null);
            String id = keycloakService.createKeycloakUser(email);
            joinTenant.setKeycloakId(id);
            tenantRepository.save(joinTenant);
            userRoleService.createRole(joinTenant);
            PasswordRecoveryToken passwordRecoveryToken = passwordRecoveryTokenService.create(joinTenant);
            mailService.sendEmailForFlatmates(tenant, joinTenant, passwordRecoveryToken, applicationForm.getApplicationType());
            logService.saveLog(LogType.ACCOUNT_CREATED, joinTenant.getId());
        });
        return tenantMapper.toTenantModel(tenant);
    }
}
