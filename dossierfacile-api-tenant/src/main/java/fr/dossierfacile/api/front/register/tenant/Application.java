package fr.dossierfacile.api.front.register.tenant;

import com.google.common.annotations.VisibleForTesting;
import fr.dossierfacile.api.front.exception.CoTenantEmailAlreadyExists;
import fr.dossierfacile.api.front.mapper.TenantMapper;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.register.SaveStep;
import fr.dossierfacile.api.front.register.form.tenant.ApplicationFormV2;
import fr.dossierfacile.api.front.register.form.tenant.CoTenantForm;
import fr.dossierfacile.api.front.security.interfaces.ClientAuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.*;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.PasswordRecoveryToken;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.User;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.LogType;
import fr.dossierfacile.common.enums.TenantType;
import fr.dossierfacile.common.repository.ApartmentSharingRepository;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.LogService;
import fr.dossierfacile.common.service.interfaces.PartnerCallBackService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
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
    private final UserService userService;
    private final ClientAuthenticationFacade clientAuthenticationFacade;

    @Override
    @Transactional
    public TenantModel saveStep(Tenant tenant, ApplicationFormV2 applicationForm) {

        checkIfEmailIsUnique(tenant, applicationForm);

        List<Tenant> oldCoTenant = tenant.getApartmentSharing().getTenants()
                .stream()
                .filter(t -> !t.getId().equals(tenant.getId()))
                .collect(Collectors.toList());

        List<Tenant> tenantToDelete = getTenantsToDelete(oldCoTenant, applicationForm);

        List<CoTenantForm> tenantToCreate = getTenantsToCreate(oldCoTenant, applicationForm);

        List<Pair<Tenant, String>> tenantWitNewEmailToUpdate = getTenantWitNewEmailToUpdate(oldCoTenant, applicationForm);

        linkEmailToTenants(tenant, tenantWitNewEmailToUpdate);

        return saveStep(tenant, applicationForm.getApplicationType(), tenantToDelete, tenantToCreate);
    }

    @VisibleForTesting
    protected List<Tenant> getTenantsToDelete(List<Tenant> oldCoTenant, ApplicationFormV2 applicationForm) {
        return oldCoTenant.stream()
                .filter(t -> applicationForm.getCoTenants().parallelStream()
                        .noneMatch(currentTenant -> Objects.equals(t.getFirstName(), currentTenant.getFirstName())
                                && Objects.equals(t.getLastName(), currentTenant.getLastName())
                                && (StringUtils.isBlank(t.getEmail()) || t.getEmail().equals(currentTenant.getEmail()))))
                .collect(Collectors.toList());
    }

    @VisibleForTesting
    protected List<CoTenantForm> getTenantsToCreate(List<Tenant> oldCoTenant, ApplicationFormV2 applicationForm) {
        return applicationForm.getCoTenants().stream()
                .filter(currentTenant -> oldCoTenant.parallelStream()
                        .noneMatch(oldTenant -> Objects.equals(oldTenant.getFirstName(), currentTenant.getFirstName())
                                && Objects.equals(oldTenant.getLastName(), currentTenant.getLastName())
                                && (StringUtils.isBlank(oldTenant.getEmail()) || oldTenant.getEmail().equals(currentTenant.getEmail()))))
                .collect(Collectors.toList());
    }

    protected List<Pair<Tenant, String>> getTenantWitNewEmailToUpdate(List<Tenant> oldCoTenant, ApplicationFormV2 applicationForm) {
        return applicationForm.getCoTenants().stream()
                .map(currentTenant -> {
                            Optional<Tenant> updatedTenant = oldCoTenant.parallelStream()
                                    .filter(oldTenant ->
                                            Objects.equals(oldTenant.getFirstName(), currentTenant.getFirstName())
                                                    && Objects.equals(oldTenant.getLastName(), currentTenant.getLastName())
                                                    && !StringUtils.equals(oldTenant.getEmail(), currentTenant.getEmail())
                                                    && oldTenant.getEmail() == null
                                    ).findFirst();
                            return updatedTenant.map(tenant -> new ImmutablePair<>(tenant, currentTenant.getEmail())).orElse(null);
                        }
                )
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @VisibleForTesting
    protected void linkEmailToTenants(Tenant tenantCreate, List<Pair<Tenant, String>> tenantWitNewEmailToUpdate) {
        if (tenantWitNewEmailToUpdate != null) {
            List<String> emailsExistTenants = tenantWitNewEmailToUpdate.stream()
                    .map(Pair::getRight)
                    .filter(tenantRepository::existsByEmail)
                    .collect(Collectors.toList());

            if (!emailsExistTenants.isEmpty())
                throw new IllegalArgumentException("Cannot update a tenant with an existing email: " + String.join(",", emailsExistTenants));

            //tenantWitNewEmailToUpdate
            for (Pair<Tenant, String> pair : tenantWitNewEmailToUpdate) {
                Tenant t = pair.getLeft();
                String newEmail = pair.getRight();

                if (StringUtils.isNotBlank(newEmail)) {
                    String keycloakId = keycloakService.createKeycloakUser(newEmail);
                    Tenant existingTenant = tenantRepository.findByKeycloakId(keycloakId);
                    if (existingTenant != null) {
                        // A tenant already exists, should never happen here because we have already checked existing email
                        String msg = "Cannot update a tenant with an existing email: " + String.join(",", emailsExistTenants);
                        throw new IllegalArgumentException(msg);
                    }

                    t.setKeycloakId(keycloakId);
                    t.setEmail(newEmail);
                    userRoleService.createRole(t);
                    tenantRepository.save(t);

                    PasswordRecoveryToken passwordRecoveryToken = passwordRecoveryTokenService.create(t);
                    mailService.sendEmailForFlatmates(tenantCreate, t, passwordRecoveryToken, tenantCreate.getApartmentSharing().getApplicationType());
                }
            }
        }
    }

    TenantModel saveStep(Tenant tenant, ApplicationType newApplicationType, List<Tenant> tenantToDelete, List<CoTenantForm> tenantToCreate) {
        ApartmentSharing apartmentSharing = tenant.getApartmentSharing();
        ApplicationType currentApplicationType = apartmentSharing.getApplicationType();
        apartmentSharing.setApplicationType(newApplicationType);
        apartmentSharingService.resetDossierPdfGenerated(apartmentSharing);

        deleteCoTenants(tenantToDelete);
        logService.saveApplicationTypeChangedLog(apartmentSharing.getTenants(), currentApplicationType, newApplicationType);
        createCoTenants(tenant, tenantToCreate, apartmentSharing);

        LocalDateTime now = LocalDateTime.now();
        tenant.lastUpdateDateProfile(now, null);
        tenantRepository.save(tenant);
        return tenantMapper.toTenantModel(tenant, (!clientAuthenticationFacade.isClient()) ? null : clientAuthenticationFacade.getClient());
    }

    private void createCoTenants(Tenant tenantCreate, List<CoTenantForm> tenants, ApartmentSharing apartmentSharing) {
        // check if email account exist
        // Currently we cannot add existing user
        List<String> emailsExistTenants = tenants.stream()
                .map(CoTenantForm::getEmail)
                .filter(Objects::nonNull)
                .map(String::toLowerCase)
                .filter(StringUtils::isNotBlank)
                .filter(tenantRepository::existsByEmail)
                .collect(Collectors.toList());

        if (!emailsExistTenants.isEmpty())
            throw new IllegalArgumentException("Cannot add tenant with existing emails " + String.join(",", emailsExistTenants));

        Set<Tenant> joinTenants = tenants.stream().map(
                tenant -> {
                    Tenant joinTenant = Tenant.builder()
                            .tenantType(TenantType.JOIN)
                            .firstName(tenant.getFirstName())
                            .lastName(tenant.getLastName())
                            .preferredName(tenant.getPreferredName())
                            .email(StringUtils.isBlank(tenant.getEmail()) ? null : tenant.getEmail())
                            .apartmentSharing(apartmentSharing)
                            .build();
                    if (apartmentSharing.getApplicationType() == ApplicationType.COUPLE) {
                        joinTenant.setHonorDeclaration(tenantCreate.getHonorDeclaration());
                    }
                    tenantRepository.save(joinTenant);

                    if (apartmentSharing.getApplicationType() == ApplicationType.COUPLE) {
                        if (!CollectionUtils.isEmpty(tenantCreate.getTenantsUserApi())) {
                            tenantCreate.getTenantsUserApi()
                                    .forEach(tenantUserApi -> {
                                        partnerCallBackService.registerTenant(joinTenant, tenantUserApi.getUserApi());
                                    });
                        }
                    }

                    if (StringUtils.isNotBlank(tenant.getEmail())) {
                        // create keycloak user
                        String newKeycloakId = keycloakService.createKeycloakUser(tenant.getEmail());
                        Tenant existingTenant = tenantRepository.findByKeycloakId(newKeycloakId);
                        if (existingTenant != null) {
                            // A tenant already exists, should never happen here because we cannot add existing user
                            tenantRepository.delete(joinTenant);
                            String msg = "Cannot add a cotenant with an existing account: " + String.join(",", emailsExistTenants);
                            throw new IllegalArgumentException(msg);
                        }
                        joinTenant.setKeycloakId(newKeycloakId);
                        userRoleService.createRole(joinTenant);
                        tenantRepository.save(joinTenant);

                        PasswordRecoveryToken passwordRecoveryToken = passwordRecoveryTokenService.create(joinTenant);
                        mailService.sendEmailForFlatmates(tenantCreate, joinTenant, passwordRecoveryToken, apartmentSharing.getApplicationType());
                    }

                    // Relating all the clients related to tenant CREATE to tenant JOIN
                    Optional.ofNullable(tenantCreate.getTenantsUserApi())
                            .orElse(new ArrayList<>())
                            .forEach(tenantUserApi -> partnerCallBackService.registerTenant(joinTenant, tenantUserApi.getUserApi()));

                    logService.saveLog(LogType.ACCOUNT_CREATED, joinTenant.getId());
                    return joinTenant;
                }
        ).collect(Collectors.toSet());

        apartmentSharing.getTenants().addAll(joinTenants);
        apartmentSharingRepository.save(apartmentSharing);
    }

    private void checkIfEmailIsUnique(Tenant tenant, ApplicationFormV2 applicationForm) {
        var emails = applicationForm.getCoTenants().stream()
                .map(CoTenantForm::getEmail)
                .filter(StringUtils::isNotBlank)
                .toList();

        if (emails.isEmpty()) {
            return;
        }

        var existingEmails = tenantRepository.findByEmailInAndApartmentSharingNot(emails, tenant.getApartmentSharing())
                .stream()
                .map(User::getEmail)
                .toList();

        if (!existingEmails.isEmpty()) {
            throw new CoTenantEmailAlreadyExists();
        }
    }

    private void deleteCoTenants(List<Tenant> tenantToDelete) {
        tenantToDelete.forEach(userService::deleteAccount);
    }

}
