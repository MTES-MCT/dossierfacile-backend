package fr.dossierfacile.api.front.register.tenant;

import fr.dossierfacile.api.front.mapper.TenantMapper;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.register.SaveStep;
import fr.dossierfacile.api.front.register.form.tenant.ApplicationFormV2;
import fr.dossierfacile.api.front.register.form.tenant.CoTenantForm;
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
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.LogType;
import fr.dossierfacile.common.enums.PartnerCallBackType;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.PartnerCallBackService;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class Application implements SaveStep<ApplicationFormV2> {

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
    public TenantModel saveStep(Tenant tenant, ApplicationFormV2 applicationForm) {
        List<Tenant> oldCoTenant = tenant.getApartmentSharing().getTenants()
                .stream()
                .filter(t -> !t.getId().equals(tenant.getId()))
                .collect(Collectors.toList());

        List<Tenant> tenantToDelete = oldCoTenant.stream()
                .filter(t -> applicationForm.getCoTenants().parallelStream()
                        .noneMatch(currentTenant -> t.getFirstName().equals(currentTenant.getFirstName())
                                && t.getLastName().equals(currentTenant.getLastName())))
                .collect(Collectors.toList());

        List<CoTenantForm> tenantToCreate = applicationForm.getCoTenants().stream()
                .filter(currentTenant -> oldCoTenant.parallelStream()
                        .noneMatch(oldTenant -> oldTenant.getFirstName().equals(currentTenant.getFirstName())
                                && oldTenant.getLastName().equals(currentTenant.getLastName())))
                .collect(Collectors.toList());


        return saveStep(tenant, applicationForm.getApplicationType(), tenantToDelete, tenantToCreate);
    }

    TenantModel saveStep(Tenant tenant, ApplicationType applicationType, List<Tenant> tenantToDelete, List<CoTenantForm> tenantToCreate) {
        //tenant.getApartmentSharing().getTenants()
        ApartmentSharing apartmentSharing = tenant.getApartmentSharing();
        apartmentSharing.setApplicationType(applicationType);
        apartmentSharingService.resetDossierPdfGenerated(apartmentSharing);

        deleteCoTenant(tenant, tenantToDelete, apartmentSharing);
        createCoTenant(tenant, tenantToCreate, apartmentSharing);

        LocalDateTime now = LocalDateTime.now();
        tenant.lastUpdateDateProfile(now, null);
        tenantRepository.save(tenant);
        return tenantMapper.toTenantModel(tenant);
    }

    private void createCoTenant(Tenant tenantCreate, List<CoTenantForm> tenants, ApartmentSharing apartmentSharing) {
        // check if email account exist
        // Currently we cannot add existing user
        List<String> emailsExistTenants = tenants.stream()
                .filter(tenant -> StringUtils.isNotBlank(tenant.getEmail()))
                .filter(tenant -> tenantRepository.existsByEmail(tenant.getEmail()))
                .map(tenant -> tenant.getEmail())
                .collect(Collectors.toList());

        if (!emailsExistTenants.isEmpty())
            throw new IllegalArgumentException("Cannot add tenant with existing emails " + String.join(",", emailsExistTenants));

        Set<Tenant> joinTenants = tenants.stream().map(
                tenant -> {
                    Tenant joinTenant = new Tenant(
                            tenant.getFirstName(),
                            tenant.getLastName(),
                            tenant.getPreferredName(),
                            StringUtils.isBlank(tenant.getEmail()) ? null : tenant.getEmail(),
                            apartmentSharing);
                    if (Boolean.TRUE.equals(tenantCreate.getHonorDeclaration())){
                        joinTenant.setHonorDeclaration(true);
                    }
                    tenantRepository.save(joinTenant);

                    if (StringUtils.isNotBlank(tenant.getEmail())) {
                        // create keycloak user
                        joinTenant.setKeycloakId(keycloakService.createKeycloakUser(tenant.getEmail()));
                        userRoleService.createRole(joinTenant);
                        tenantRepository.save(joinTenant);

                        PasswordRecoveryToken passwordRecoveryToken = passwordRecoveryTokenService.create(joinTenant);
                        mailService.sendEmailForFlatmates(tenantCreate, joinTenant, passwordRecoveryToken, apartmentSharing.getApplicationType());
                    }

                    // Relating all the clients related to tenant CREATE to tenant JOIN
                    Optional.ofNullable(tenantCreate.getTenantsUserApi())
                            .orElse(new ArrayList<>())
                            .forEach(tenantUserApi -> partnerCallBackService.linkTenantToPartner(null, joinTenant, tenantUserApi.getUserApi()));

                    logService.saveLog(LogType.ACCOUNT_CREATED, joinTenant.getId());
                    return joinTenant;
                }
        ).collect(Collectors.toSet());

        apartmentSharing.getTenants().addAll(joinTenants);
        apartmentSharingRepository.save(apartmentSharing);
    }

    private void deleteCoTenant(Tenant tenantCreate, List<Tenant> tenantToDelete, ApartmentSharing apartmentSharing) {

        partnerCallBackService.sendCallBack(tenantToDelete, PartnerCallBackType.DELETED_ACCOUNT);
        keycloakService.deleteKeycloakUsers(tenantToDelete);

        tenantRepository.deleteAll(tenantToDelete);
        tenantRepository.save(tenantCreate);

        apartmentSharing.getTenants().removeAll(tenantToDelete);
        apartmentSharingRepository.save(apartmentSharing);
    }


}
