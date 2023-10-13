package fr.gouv.bo.service;

import com.google.gson.Gson;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.BOUser;
import fr.dossierfacile.common.entity.Log;
import fr.dossierfacile.common.entity.PropertyApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.UserRole;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.LogType;
import fr.dossierfacile.common.enums.PartnerCallBackType;
import fr.dossierfacile.common.enums.Role;
import fr.dossierfacile.common.enums.TenantType;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.PartnerCallBackService;
import fr.gouv.bo.mapper.TenantMapper;
import fr.gouv.bo.repository.BOApartmentSharingRepository;
import fr.gouv.bo.repository.BOUserRepository;
import fr.gouv.bo.repository.PropertyApartmentSharingRepository;
import fr.gouv.bo.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Slf4j
public class UserService {

    private final Gson gson;
    private final BOUserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final TenantCommonRepository tenantRepository;
    private final MailService mailService;
    private final TenantMapper tenantMapper;
    private final KeycloakService keycloakService;
    private final BOApartmentSharingRepository apartmentSharingRepository;
    private final PropertyApartmentSharingRepository propertyApartmentSharingRepository;
    private final ApartmentSharingService apartmentSharingService;
    private final PartnerCallBackService partnerCallBackService;
    private final LogService logService;

    @Value("${authorize.domain.bo}")
    private String ad;

    public List<BOUser> findAll() {
        return userRepository.findAll(Sort.by("email"));
    }

    public BOUser findUserByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    public void delete(Long id) {
        userRepository.deleteById(id);
    }

    public void deleteCoTenant(Tenant tenant, BOUser operator) {

        if (tenant.getTenantType().equals(TenantType.CREATE)) {
            throw new IllegalArgumentException("this tenant is a main tenant");
        }

        ApartmentSharing apartmentSharing = tenant.getApartmentSharing();
        partnerCallBackService.sendCallBack(tenant, PartnerCallBackType.DELETED_ACCOUNT);
        if (tenant.getKeycloakId() != null) {
            keycloakService.deleteKeycloakSingleUser(tenant);
        }
        saveAndDeleteInfoByTenant(tenant, operator);
        tenantRepository.delete(tenant);

        apartmentSharing.getTenants().remove(tenant);
        updateApplicationTypeOfApartmentAfterDeletionOfCotenant(apartmentSharing);
        apartmentSharingService.refreshUpdateDate(apartmentSharing);
        apartmentSharingService.resetDossierPdfGenerated(apartmentSharing);
    }

    private void saveAndDeleteInfoByTenant(Tenant tenant, BOUser operator) {
        mailService.sendEmailAccountDeleted(tenant);
        logService.saveByLog(
                Log.builder()
                        .logType(LogType.ACCOUNT_DELETE)
                        .tenantId(tenant.getId())
                        .operatorId(operator.getId())
                        .jsonProfile(tenantMapper.toTenantModel(tenant))
                        .creationDateTime(LocalDateTime.now())
                        .build());
    }

    public Tenant setAsTenantCreate(Tenant tenant) {
        ApartmentSharing apartmentSharing = tenant.getApartmentSharing();
        UserRepresentation keycloakUser = keycloakService.getKeyCloakUser(
                tenant.getKeycloakId() == null ? null : tenant.getKeycloakId());

        if (keycloakUser == null || !keycloakUser.isEnabled()) { // cannot be a create tenant
            throw new IllegalStateException("This tenant cannot be a tenant - missing keycloak user or is not enabled");
        }
        Tenant mainTenant = apartmentSharing.getTenants().stream()
                .filter(t -> t.getTenantType().equals(TenantType.CREATE))
                .findFirst()
                .orElseThrow(IllegalStateException::new);

        mainTenant.setTenantType(TenantType.JOIN);
        tenant.setTenantType(TenantType.CREATE);
        tenantRepository.save(mainTenant);
        tenantRepository.save(tenant);

        return tenant;
    }

    public void deleteApartmentSharing(Tenant tenant, BOUser operator) {
        partnerCallBackService.sendCallBack(tenant, PartnerCallBackType.DELETED_ACCOUNT);
        ApartmentSharing apartmentSharing = tenant.getApartmentSharing();
        removeFromOwner(tenant);
        keycloakService.deleteKeycloakUsers(
                apartmentSharing.getTenants().stream()
                        .filter(t -> t.getKeycloakId() != null)
                        .collect(Collectors.toList()));
        apartmentSharing.getTenants().forEach( t -> this.saveAndDeleteInfoByTenant(t, operator));
        apartmentSharingRepository.delete(apartmentSharing);
    }

    public void removeFromOwner(Tenant tenant) {
        List<PropertyApartmentSharing> propertyApartmentSharingList = propertyApartmentSharingRepository.findPropertyApartmentSharingsByApartmentSharingId(tenant.getApartmentSharing().getId());
        if (propertyApartmentSharingList != null) {
            propertyApartmentSharingList.forEach(propertyApartmentSharingRepository::delete);
        }
    }

    private void updateApplicationTypeOfApartmentAfterDeletionOfCotenant(ApartmentSharing apartmentSharing) {
        ApplicationType nextApplicationType = ApplicationType.ALONE;

        //Current application can only be in this point [COUPLE] or [GROUP]
        ApplicationType previousApplicationType = apartmentSharing.getApplicationType();

        //If previous application was a [GROUP] and after deletion of 1 cotenant, it has now (>=2) tenants then,
        // it will stay as an application [GROUP]. Otherwise it will become an application [ALONE]
        if (previousApplicationType == ApplicationType.GROUP
                && apartmentSharing.getNumberOfTenants() >= 2) {
            nextApplicationType = ApplicationType.GROUP;
        }

        if (previousApplicationType != nextApplicationType) {
            log.info("Changing applicationType of apartment with ID [" + apartmentSharing.getId() + "] from [" + previousApplicationType.name() + "] to [" + nextApplicationType.name() + "]");
            apartmentSharing.setApplicationType(nextApplicationType);
            apartmentSharingRepository.save(apartmentSharing);
        }
    }

    @Transactional
    public void deleteRoles(BOUser user, List<Role> roles) {
        roles.stream().forEach(r -> {
            Optional<UserRole> role = user.getUserRoles().stream()
                    .filter(userRole -> userRole.getRole() == r)
                    .findFirst();
            if (role.isPresent()) {
                user.getUserRoles().remove(role.get());
                userRoleRepository.delete(role.get());
            }
        });
        userRepository.save(user);
    }

    @Transactional
    public void addRoles(BOUser user, List<Role> roles) {
        roles.stream().forEach(r -> {
            if (user.getUserRoles().stream().noneMatch(userRole -> userRole.getRole() == r)) {
                user.getUserRoles().add(userRoleRepository.save(new UserRole(user, r)));
            }
        });
        userRepository.save(user);
    }

    @Transactional
    public void createUserByEmail(String email, Role role) {
        BOUser user = BOUser.builder().email(email).build();
        addRoles(userRepository.save(user), Collections.singletonList(role));
    }
}

