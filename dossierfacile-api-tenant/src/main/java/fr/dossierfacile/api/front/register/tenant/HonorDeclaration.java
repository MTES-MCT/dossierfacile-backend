package fr.dossierfacile.api.front.register.tenant;

import fr.dossierfacile.api.front.exception.TenantIllegalStateException;
import fr.dossierfacile.api.front.mapper.TenantMapper;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.register.SaveStep;
import fr.dossierfacile.api.front.register.form.tenant.HonorDeclarationForm;
import fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService;
import fr.dossierfacile.api.front.service.interfaces.MailService;
import fr.dossierfacile.api.front.service.interfaces.TenantStatusService;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Service
@AllArgsConstructor
public class HonorDeclaration implements SaveStep<HonorDeclarationForm> {

    private final TenantCommonRepository tenantRepository;
    private final TenantMapper tenantMapper;
    private final MailService mailService;
    private final TenantStatusService tenantStatusService;
    private final ApartmentSharingService apartmentSharingService;

    private static List<Tenant> getTenantOrPartners(Tenant tenant) {
        ApartmentSharing apartmentSharing = tenant.getApartmentSharing();
        return switch (apartmentSharing.getApplicationType()) {
            case COUPLE -> new ArrayList<>(apartmentSharing.getTenants());
            case ALONE, GROUP -> singletonList(tenant);
        };
    }

    @Override
    @Transactional
    public TenantModel saveStep(Tenant tenant, HonorDeclarationForm honorDeclarationForm) {
        tenant = tenantRepository.findOneById(tenant.getId());
        tenant.setClarification(honorDeclarationForm.getClarification());
        for (Tenant t : getTenantOrPartners(tenant)) {
            checkTenantValidity(t);
            t.setHonorDeclaration(honorDeclarationForm.isHonorDeclaration());
            t.lastUpdateDateProfile(LocalDateTime.now(), null);
            tenantStatusService.updateTenantStatus(t);
        }

        Tenant tenantSaved = tenantRepository.save(tenant);

        apartmentSharingService.resetDossierPdfGenerated(tenant.getApartmentSharing());
        mailService.sendEmailAccountCompleted(tenantSaved);
        return tenantMapper.toTenantModel(tenantSaved);
    }

    private void checkTenantValidity(Tenant tenant) {
        if (isBlank(tenant.getFirstName()) || isBlank(tenant.getLastName())) {
            throw new TenantIllegalStateException("Firstname or Lastname should be filled");
        }
        if (tenant.getGuarantors().stream().anyMatch(
                g -> switch (g.getTypeGuarantor()) {
                    case NATURAL_PERSON -> isBlank(g.getFirstName()) || isBlank(g.getLastName());
                    case LEGAL_PERSON -> isBlank(g.getFirstName()) || isBlank(g.getLegalPersonName());
                    default -> false;
                })) {
            throw new TenantIllegalStateException("Guarantor's Information should be filled");
        }
    }
}
