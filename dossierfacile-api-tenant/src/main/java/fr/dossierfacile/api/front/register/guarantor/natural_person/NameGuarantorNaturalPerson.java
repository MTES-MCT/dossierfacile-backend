package fr.dossierfacile.api.front.register.guarantor.natural_person;

import fr.dossierfacile.api.front.exception.GuarantorNotFoundException;
import fr.dossierfacile.api.front.mapper.TenantMapper;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.register.SaveStep;
import fr.dossierfacile.api.front.register.form.guarantor.natural_person.NameGuarantorNaturalPersonForm;
import fr.dossierfacile.api.front.repository.GuarantorRepository;
import fr.dossierfacile.api.front.security.interfaces.ClientAuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService;
import fr.dossierfacile.api.front.service.interfaces.DocumentService;
import fr.dossierfacile.api.front.service.interfaces.TenantStatusService;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.TypeGuarantor;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.document.analysis.service.DocumentIAService;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;

@Service
@AllArgsConstructor
public class NameGuarantorNaturalPerson implements SaveStep<NameGuarantorNaturalPersonForm> {
    private final TenantCommonRepository tenantRepository;
    private final TenantMapper tenantMapper;
    private final GuarantorRepository guarantorRepository;
    private final DocumentService documentService;
    private final TenantStatusService tenantStatusService;
    private final ApartmentSharingService apartmentSharingService;
    private final ClientAuthenticationFacade clientAuthenticationFacade;
    private final DocumentIAService documentIAService;

    @Override
    @Transactional
    public TenantModel saveStep(Tenant tenant, NameGuarantorNaturalPersonForm nameGuarantorNaturalPersonForm) {
        tenant = tenantRepository.findOneById(tenant.getId());
        Guarantor guarantor = guarantorRepository.findByTenantAndTypeGuarantorAndId(tenant, TypeGuarantor.NATURAL_PERSON, nameGuarantorNaturalPersonForm.getGuarantorId())
                .orElseThrow(() -> new GuarantorNotFoundException(nameGuarantorNaturalPersonForm.getGuarantorId()));

        // special case : When optional preferred name is not set, the front send an empty string
        var newPreferredName = StringUtils.trimToNull(nameGuarantorNaturalPersonForm.getPreferredName());
        var currentPreferredName = StringUtils.trimToNull(guarantor.getPreferredName());

        boolean guarantorIdentityHasChanged = !StringUtils.equals(guarantor.getFirstName(), nameGuarantorNaturalPersonForm.getFirstName())
                || !StringUtils.equals(guarantor.getLastName(), nameGuarantorNaturalPersonForm.getLastName())
                || !StringUtils.equals(currentPreferredName, newPreferredName);

        guarantor.setFirstName(nameGuarantorNaturalPersonForm.getFirstName());
        guarantor.setLastName(nameGuarantorNaturalPersonForm.getLastName());
        guarantor.setPreferredName(nameGuarantorNaturalPersonForm.getPreferredName());
        guarantor.setTenant(tenant);
        guarantorRepository.save(guarantor);
        tenant.lastUpdateDateProfile(LocalDateTime.now(), DocumentCategory.IDENTIFICATION);

        if (guarantorIdentityHasChanged) {
            var documents = guarantor.getDocuments();
            documentService.resetValidatedOrInProgressDocumentsAccordingCategories(documents, Arrays.asList(DocumentCategory.values()));
            documents.forEach(documentIAService::analyseDocument);
        }

        tenantStatusService.updateTenantStatus(tenant);
        apartmentSharingService.resetDossierPdfGenerated(tenant.getApartmentSharing());

        return tenantMapper.toTenantModel(tenantRepository.save(tenant), (!clientAuthenticationFacade.isClient()) ? null : clientAuthenticationFacade.getClient());
    }
}
