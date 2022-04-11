package fr.gouv.bo.service;

import com.google.gson.Gson;
import fr.dossierfacile.common.entity.AccountDeleteLog;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.PropertyApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.User;
import fr.dossierfacile.common.entity.UserRole;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.AuthProvider;
import fr.dossierfacile.common.enums.PartnerCallBackType;
import fr.dossierfacile.common.enums.Role;
import fr.dossierfacile.common.enums.TenantType;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.OvhService;
import fr.dossierfacile.common.service.interfaces.PartnerCallBackService;
import fr.gouv.bo.dto.UserDTO;
import fr.gouv.bo.mapper.TenantMapper;
import fr.gouv.bo.model.tenant.TenantModel;
import fr.gouv.bo.repository.AccountDeleteLogRepository;
import fr.gouv.bo.repository.ApartmentSharingRepository;
import fr.gouv.bo.repository.PropertyApartmentSharingRepository;
import fr.gouv.bo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Slf4j
public class UserService {

    private final Gson gson;
    private final UserRepository userRepository;
    private final TenantCommonRepository tenantRepository;
    private final ModelMapper modelMapper;
    private final MailService mailService;
    private final OvhService ovhService;
    private final TenantMapper tenantMapper;
    private final AccountDeleteLogRepository accountDeleteLogRepository;
    private final KeycloakService keycloakService;
    private final ApartmentSharingRepository apartmentSharingRepository;
    private final PropertyApartmentSharingRepository propertyApartmentSharingRepository;
    private final ApartmentSharingService apartmentSharingService;
    private final PartnerCallBackService partnerCallBackService;

    @Value("${authorize.domain.bo}")
    private String ad;

    public List<User> findAllAdmins() {
        return userRepository.findAllAdmins(ad);
    }

    public User findUserByEmail(String email) {
        return userRepository.findOneByEmail(email);
    }

    public User save(UserDTO userDTO) {
        User user = modelMapper.map(userDTO, User.class);
        user.setProvider(AuthProvider.google);
        return userRepository.save(user);
    }

    public User findOne(Long id) {
        return userRepository.getOne(id);
    }

    public User update(UserDTO userDTO) {
        User user = findOne(userDTO.getId());
        user.setFirstName(userDTO.getFirstName());
        user.setLastName(userDTO.getLastName());
        Set<UserRole> userRoleSet = new HashSet<>();
        for (Role role : userDTO.getRole()) {
            userRoleSet.add(new UserRole(user, role));
        }
        user.setUserRoles(userRoleSet);
        return userRepository.save(user);
    }

    public void delete(Long id) {
        userRepository.deleteById(id);
    }

    public void deleteCoTenant(Tenant tenant, Long id) {

        if (tenant.getTenantType().equals(TenantType.CREATE)) {
            ApartmentSharing apartmentSharing = tenant.getApartmentSharing();
            Tenant coTenant = apartmentSharing.getTenants().stream().filter(t -> t.getId().equals(id) && t.getTenantType().equals(TenantType.JOIN)).findFirst().orElseThrow(null);
            if (coTenant != null) {
                partnerCallBackService.sendCallBack(coTenant, PartnerCallBackType.DELETED_ACCOUNT);
                if (coTenant.getKeycloakId() != null) {
                    keycloakService.deleteKeycloakSingleUser(coTenant);
                }
                saveAndDeleteInfoByTenant(coTenant);
                userRepository.delete(coTenant);
                apartmentSharing.getTenants().remove(coTenant);
                updateApplicationTypeOfApartmentAfterDeletionOfCotenant(apartmentSharing);
                apartmentSharingService.resetDossierPdfGenerated(apartmentSharing);
            }
        }
    }

    private void saveAndDeleteInfoByTenant(Tenant tenant) {
        mailService.sendEmailAccountDeleted(tenant);
        this.savingJsonProfileBeforeDeletion(tenantMapper.toTenantModel(tenant));

        Optional.ofNullable(tenant.getDocuments())
                .orElse(new ArrayList<>())
                .forEach(this::deleteFilesFromStorage);
        Optional.ofNullable(tenant.getGuarantors())
                .orElse(new ArrayList<>())
                .forEach(guarantor -> Optional.ofNullable(guarantor.getDocuments())
                        .orElse(new ArrayList<>())
                        .forEach(this::deleteFilesFromStorage)
                );
    }

    public Tenant deleteAndReplaceTenantCreate(Tenant create) {
        ApartmentSharing apartmentSharing = create.getApartmentSharing();
        Tenant mainTenant = apartmentSharing.getTenants().stream().filter(tenant -> !tenant.getTenantType().equals(TenantType.CREATE)).findAny().orElseThrow(null);
        if (mainTenant != null) {
            partnerCallBackService.sendCallBack(create, PartnerCallBackType.DELETED_ACCOUNT);
            if (create.getKeycloakId() != null) {
                keycloakService.deleteKeycloakSingleUser(create);
            }
            saveAndDeleteInfoByTenant(create);
            tenantRepository.delete(create);
            apartmentSharing.getTenants().remove(create);
            mainTenant.setTenantType(TenantType.CREATE);
            tenantRepository.save(mainTenant);
            updateApplicationTypeOfApartmentAfterDeletionOfCotenant(apartmentSharing);
            apartmentSharingService.resetDossierPdfGenerated(apartmentSharing);
        }
        return mainTenant;
    }

    private void savingJsonProfileBeforeDeletion(TenantModel tenantModel) {
        accountDeleteLogRepository.save(
                AccountDeleteLog.builder()
                        .userId(tenantModel.getId())
                        .deletionDate(LocalDateTime.now())
                        .jsonProfileBeforeDeletion(gson.toJson(tenantModel))
                        .build()
        );
    }

    private void deleteFilesFromStorage(Document document) {
        List<File> files = document.getFiles();
        if (files != null && !files.isEmpty()) {
            log.info("Removing files from storage of document with id [" + document.getId() + "]");
            ovhService.delete(files.stream().map(File::getPath).collect(Collectors.toList()));
        }
        if (document.getName() != null && !document.getName().isBlank()) {
            log.info("Removing document from storage with path [" + document.getName() + "]");
            ovhService.delete(document.getName());
        }
    }

    public void deleteApartmentSharing(Tenant tenant) {
        ApartmentSharing apartmentSharing = tenant.getApartmentSharing();
        removeFromOwner(tenant);
        if (apartmentSharing.getTenants().size() == 1 && tenant.getKeycloakId() != null) {
            keycloakService.deleteKeycloakSingleUser(tenant);
        }
        if (apartmentSharing.getTenants().size() > 1) {
            keycloakService.deleteKeycloakUsers(apartmentSharing.getTenants());
        }
        apartmentSharing.getTenants().forEach(this::saveAndDeleteInfoByTenant);
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
}

