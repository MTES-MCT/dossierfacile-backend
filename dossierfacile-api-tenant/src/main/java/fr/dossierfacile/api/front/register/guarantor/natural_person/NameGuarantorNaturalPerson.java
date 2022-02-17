package fr.dossierfacile.api.front.register.guarantor.natural_person;

import fr.dossierfacile.api.front.exception.GuarantorNotFoundException;
import fr.dossierfacile.api.front.mapper.TenantMapper;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.register.SaveStep;
import fr.dossierfacile.api.front.register.form.guarantor.natural_person.NameGuarantorNaturalPersonForm;
import fr.dossierfacile.api.front.repository.GuarantorRepository;
import fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService;
import fr.dossierfacile.api.front.service.interfaces.DocumentService;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.TypeGuarantor;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@AllArgsConstructor
public class NameGuarantorNaturalPerson implements SaveStep<NameGuarantorNaturalPersonForm> {
    private final TenantCommonRepository tenantRepository;
    private final TenantMapper tenantMapper;
    private final GuarantorRepository guarantorRepository;
    private final DocumentService documentService;
    private final TenantService tenantService;
    private final ApartmentSharingService apartmentSharingService;

    @Override
    @Transactional
    public TenantModel saveStep(Tenant tenant, NameGuarantorNaturalPersonForm nameGuarantorNaturalPersonForm) {
        documentService.resetValidatedDocumentsStatusToToProcess(tenant);
        Guarantor guarantor = guarantorRepository.findByTenantAndTypeGuarantorAndId(tenant, TypeGuarantor.NATURAL_PERSON, nameGuarantorNaturalPersonForm.getGuarantorId())
                .orElseThrow(() -> new GuarantorNotFoundException(nameGuarantorNaturalPersonForm.getGuarantorId()));
        guarantor.setFirstName(nameGuarantorNaturalPersonForm.getFirstName());
        guarantor.setLastName(nameGuarantorNaturalPersonForm.getLastName());
        guarantor.setTenant(tenant);
        guarantorRepository.save(guarantor);
        tenant.lastUpdateDateProfile(LocalDateTime.now(), DocumentCategory.IDENTIFICATION);
        tenant.getGuarantors().forEach(guarantor1 -> documentService.resetValidatedAndDeniedDocumentsStatusToToProcess(guarantor1.getDocuments()));
        tenantService.updateTenantStatus(tenant);
        apartmentSharingService.resetDossierPdfGenerated(tenant.getApartmentSharing());
        return tenantMapper.toTenantModel(tenantRepository.save(tenant));
    }
}
