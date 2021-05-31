package fr.dossierfacile.api.front.register.tenant;

import fr.dossierfacile.api.front.mapper.TenantMapper;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.register.SaveStep;
import fr.dossierfacile.api.front.register.form.tenant.ApplicationForm;
import fr.dossierfacile.api.front.repository.ApartmentSharingRepository;
import fr.dossierfacile.api.front.repository.TenantRepository;
import fr.dossierfacile.api.front.service.interfaces.DocumentService;
import fr.dossierfacile.api.front.service.interfaces.MailService;
import fr.dossierfacile.api.front.service.interfaces.PasswordRecoveryTokenService;
import fr.dossierfacile.api.front.service.interfaces.UserRoleService;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.PasswordRecoveryToken;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.TenantType;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class Application implements SaveStep<ApplicationForm> {

    private final TenantRepository tenantRepository;
    private final UserRoleService userRoleService;
    private final TenantMapper tenantMapper;
    private final MailService mailService;
    private final PasswordRecoveryTokenService passwordRecoveryTokenService;
    private final ApartmentSharingRepository apartmentSharingRepository;
    private final DocumentService documentService;

    @Override
    @Transactional
    public TenantModel saveStep(Tenant tenant, ApplicationForm applicationForm) {
        tenant.setTenantType(TenantType.CREATE);
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
        tenantRepository.deleteAll(tenantRepository.findByListEmail(tenantToDelete));

        LocalDateTime now = LocalDateTime.now();
        tenant.lastUpdateDateProfile(now, null);
        tenantRepository.save(tenant);

        tenantToCreate.forEach(email -> {
            Tenant joinTenant = new Tenant(email, apartmentSharing);
            joinTenant.lastUpdateDateProfile(now, null);
            tenantRepository.save(joinTenant);
            userRoleService.createRole(joinTenant);
            PasswordRecoveryToken passwordRecoveryToken = passwordRecoveryTokenService.create(joinTenant);
            mailService.sendEmailForFlatmates(tenant, joinTenant, passwordRecoveryToken, applicationForm.getApplicationType());
        });
        documentService.updateOthersDocumentsStatus(tenant);
        return tenantMapper.toTenantModel(tenant);
    }
}
