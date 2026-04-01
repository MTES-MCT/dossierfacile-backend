package fr.dossierfacile.api.front.register.tenant;

import fr.dossierfacile.api.front.mapper.TenantMapper;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.register.SaveStep;
import fr.dossierfacile.api.front.register.form.tenant.NamesForm;
import fr.dossierfacile.api.front.security.interfaces.ClientAuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService;
import fr.dossierfacile.api.front.service.interfaces.DocumentService;
import fr.dossierfacile.api.front.service.interfaces.TenantStatusService;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.TypeGuarantor;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.document.analysis.service.DocumentIAService;
import lombok.AllArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@AllArgsConstructor
public class Names implements SaveStep<NamesForm> {

    private final TenantCommonRepository tenantRepository;
    private final TenantMapper tenantMapper;
    private final ApartmentSharingService apartmentSharingService;
    private final DocumentService documentService;
    private final TenantStatusService tenantStatusService;
    private final ClientAuthenticationFacade clientAuthenticationFacade;
    private final DocumentIAService documentIAService;

    @Override
    @Transactional
    public TenantModel saveStep(Tenant tenant, NamesForm namesForm) {
        // When preferred is not set, the front send an empty string that will cause trouble when comparing
        var preferredName = StringUtils.trimToNull(namesForm.getPreferredName());
        var tenantPreferredName = StringUtils.trimToNull(tenant.getPreferredName());

        boolean tenantIdentityHasChanged = !StringUtils.equals(tenant.getFirstName(), namesForm.getFirstName())
            || !StringUtils.equals(tenant.getLastName(), namesForm.getLastName())
            || !StringUtils.equals(tenantPreferredName, preferredName);

        // any change of first, last and preferred names triggers a document status reset and re-analysis
        List<Document> documentsToProcess = List.of();
        
        if (tenantIdentityHasChanged) {
            documentsToProcess = new ArrayList<>(CollectionUtils.emptyIfNull(tenant.getDocuments()));
            if (CollectionUtils.isNotEmpty(tenant.getGuarantors())
                    && (tenant.getGuarantors().getFirst().getTypeGuarantor() == TypeGuarantor.LEGAL_PERSON
                    || tenant.getGuarantors().getFirst().getTypeGuarantor() == TypeGuarantor.ORGANISM)) {
                documentsToProcess.addAll(tenant.getGuarantors().getFirst().getDocuments());
            }
            documentService.resetValidatedOrInProgressDocumentsAccordingCategories(documentsToProcess, Arrays.asList(DocumentCategory.values()));
            documentsToProcess.forEach(documentIAService::analyseDocument);
        }

        tenant.setOwnerType(namesForm.getOwnerType());
        tenant.setFirstName(namesForm.getFirstName());
        tenant.setLastName(namesForm.getLastName());
        tenant.setPreferredName(namesForm.getPreferredName());

        tenant.setZipCode(namesForm.getZipCode());
        tenant.setAbroad(namesForm.getAbroad());
        tenant.lastUpdateDateProfile(LocalDateTime.now(), null);
        apartmentSharingService.resetDossierPdfGenerated(tenant.getApartmentSharing());
        tenant = tenantStatusService.updateTenantStatus(tenant);
        tenant = tenantRepository.save(tenant);

        return tenantMapper.toTenantModel(tenant, (!clientAuthenticationFacade.isClient()) ? null : clientAuthenticationFacade.getClient());
    }
}
