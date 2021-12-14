package fr.dossierfacile.api.front.register.tenant;

import fr.dossierfacile.api.front.amqp.Producer;
import fr.dossierfacile.api.front.mapper.TenantMapper;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.register.SaveStep;
import fr.dossierfacile.api.front.register.form.tenant.HonorDeclarationForm;
import fr.dossierfacile.api.front.repository.TenantRepository;
import fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService;
import fr.dossierfacile.api.front.service.interfaces.DocumentService;
import fr.dossierfacile.api.front.service.interfaces.MailService;
import fr.dossierfacile.api.front.service.interfaces.PartnerCallBackService;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.common.entity.Tenant;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@AllArgsConstructor
public class HonorDeclaration implements SaveStep<HonorDeclarationForm> {

    private final TenantRepository tenantRepository;
    private final TenantMapper tenantMapper;
    private final PartnerCallBackService partnerCallBackService;
    private final Producer producer;
    private final MailService mailService;
    private final DocumentService documentService;
    private final TenantService tenantService;
    private final ApartmentSharingService apartmentSharingService;

    @Override
    @Transactional
    public TenantModel saveStep(Tenant tenant, HonorDeclarationForm honorDeclarationForm) {
        tenant.setHonorDeclaration(honorDeclarationForm.isHonorDeclaration());
        tenant.setClarification(honorDeclarationForm.getClarification());
        partnerCallBackService.sendCallBack(tenant);
        tenant.lastUpdateDateProfile(LocalDateTime.now(), null);
        producer.processFileOcr(tenant.getId());
        documentService.resetValidatedDocumentsStatusToToProcess(tenant);
        tenantService.updateTenantStatus(tenant);
        Tenant tenantSaved = tenantRepository.save(tenant);
        apartmentSharingService.resetDossierPdfGenerated(tenant.getApartmentSharing());
        mailService.sendEmailAccountCompleted(tenantSaved);
        return tenantMapper.toTenantModel(tenantSaved);
    }
}
