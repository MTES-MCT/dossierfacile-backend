package fr.dossierfacile.api.front.register.tenant;

import fr.dossierfacile.api.front.exception.TenantIllegalStateException;
import fr.dossierfacile.api.front.mapper.TenantMapper;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.register.SaveStep;
import fr.dossierfacile.api.front.register.form.tenant.HonorDeclarationForm;
import fr.dossierfacile.api.front.security.interfaces.ClientAuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService;
import fr.dossierfacile.api.front.service.interfaces.MailService;
import fr.dossierfacile.api.front.service.interfaces.TenantStatusService;
import fr.dossierfacile.common.dto.mail.TenantDto;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.enums.TypeGuarantor;
import fr.dossierfacile.common.mapper.mail.TenantMapperForMail;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.LogService;
import fr.dossierfacile.common.utils.TransactionalUtil;
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
    private final TenantMapperForMail tenantMapperForMail;
    private final ClientAuthenticationFacade clientAuthenticationFacade;
    private final LogService logService;

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
            boolean firstSubmission = !Boolean.TRUE.equals(t.getHonorDeclaration());
            t.setHonorDeclaration(honorDeclarationForm.isHonorDeclaration());
            t.lastUpdateDateProfile(LocalDateTime.now(), null);
            tenantStatusService.updateTenantStatus(t);
            if (firstSubmission) {
                notifyGuarantors(t);
            }
        }

        tenant = tenantRepository.save(tenant);
        apartmentSharingService.resetDossierPdfGenerated(tenant.getApartmentSharing());

        TenantDto tenantDto = tenantMapperForMail.toDto(tenant);
        // The mail depends on the resulting status, not on the feature flag: dossiers
        // outside the opt-in MVP keep receiving the current "waiting for review" email
        boolean completedWithoutValidation = tenant.getStatus() == TenantFileStatus.COMPLETED;
        TransactionalUtil.afterCommit(() -> {
            if (completedWithoutValidation) {
                mailService.sendEmailAccountCompletedOptin(tenantDto);
            } else {
                mailService.sendEmailAccountCompleted(tenantDto);
            }
        });

        return tenantMapper.toTenantModel(tenant, (!clientAuthenticationFacade.isClient()) ? null : clientAuthenticationFacade.getClient());

    }

    /**
     * First submission of this tenant's dossier: notify their natural-person guarantors.
     * Emails set or changed after submission are notified by NameGuarantorNaturalPerson.
     */
    private void notifyGuarantors(Tenant tenant) {
        for (Guarantor guarantor : tenant.getGuarantors()) {
            if (guarantor.getTypeGuarantor() == TypeGuarantor.NATURAL_PERSON && guarantor.getEmail() != null) {
                logService.saveGuarantorNotifiedLog(guarantor);
                String guarantorEmail = guarantor.getEmail();
                String guarantorName = guarantor.getCompleteName();
                TransactionalUtil.afterCommit(() -> mailService.sendEmailToGuarantor(guarantorEmail, guarantorName, tenant));
            }
        }
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
