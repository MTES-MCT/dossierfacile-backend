package fr.dossierfacile.api.front.register.tenant;

import fr.dossierfacile.api.front.amqp.Producer;
import fr.dossierfacile.api.front.mapper.TenantMapper;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.register.SaveStep;
import fr.dossierfacile.api.front.register.form.tenant.HonorDeclarationForm;
import fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService;
import fr.dossierfacile.api.front.service.interfaces.MailService;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static java.util.Collections.singletonList;

@Service
@AllArgsConstructor
public class HonorDeclaration implements SaveStep<HonorDeclarationForm> {

    private final TenantCommonRepository tenantRepository;
    private final TenantMapper tenantMapper;
    private final Producer producer;
    private final MailService mailService;
    private final TenantService tenantService;
    private final ApartmentSharingService apartmentSharingService;

    @Override
    @Transactional
    public TenantModel saveStep(Tenant tenant, HonorDeclarationForm honorDeclarationForm) {
        updateHonorDeclaration(tenant, honorDeclarationForm.isHonorDeclaration());
        tenant.setClarification(honorDeclarationForm.getClarification());

        getTenantOrPartners(tenant)
                .forEach(t -> {
                    t.lastUpdateDateProfile(LocalDateTime.now(), null);
                    tenantService.updateTenantStatus(t);
                });
        Tenant tenantSaved = tenantRepository.save(tenant);

        sendFileToBeProcessed(tenant);
        apartmentSharingService.resetDossierPdfGenerated(tenant.getApartmentSharing());
        mailService.sendEmailAccountCompleted(tenantSaved);
        return tenantMapper.toTenantModel(tenantSaved);
    }

    private static void updateHonorDeclaration(Tenant loggedTenant, boolean honorDeclaration) {
        getTenantOrPartners(loggedTenant)
                .forEach(tenant -> tenant.setHonorDeclaration(honorDeclaration));
    }

    private void sendFileToBeProcessed(Tenant loggedTenant) {
        getTenantOrPartners(loggedTenant)
                .forEach(tenant -> producer.processFileTax(tenant.getId()));
    }

    private static List<Tenant> getTenantOrPartners(Tenant tenant) {
        ApartmentSharing apartmentSharing = tenant.getApartmentSharing();
        return switch (apartmentSharing.getApplicationType()) {
            case COUPLE -> apartmentSharing.getTenants();
            case ALONE, GROUP -> singletonList(tenant);
        };
    }
}
