package fr.dossierfacile.api.front.register.tenant;

import fr.dossierfacile.api.front.mapper.TenantMapper;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.register.SaveStep;
import fr.dossierfacile.api.front.register.form.tenant.NamesForm;
import fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService;
import fr.dossierfacile.api.front.service.interfaces.DocumentService;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;

@Service
@AllArgsConstructor
public class Names implements SaveStep<NamesForm> {

    private final TenantCommonRepository tenantRepository;
    private final TenantMapper tenantMapper;
    private final ApartmentSharingService apartmentSharingService;
    private final DocumentService documentService;
    private final TenantService tenantService;

    @Override
    @Transactional
    public TenantModel saveStep(Tenant tenant, NamesForm namesForm) {
        //only the change of first and last names will trigger to update from validated -> to_process
        if (!tenant.getFirstName().equals(namesForm.getFirstName())
                || !tenant.getLastName().equals(namesForm.getLastName())) {
            documentService.resetValidatedDocumentsStatusOfSpecifiedCategoriesToToProcess(tenant.getDocuments(), Arrays.asList(DocumentCategory.values()));
        }
        tenant.setFirstName(namesForm.getFirstName());
        tenant.setLastName(namesForm.getLastName());
        tenant.setPreferredName(namesForm.getPreferredName());
        tenant.setZipCode(namesForm.getZipCode());
        tenant.lastUpdateDateProfile(LocalDateTime.now(), null);
        apartmentSharingService.resetDossierPdfGenerated(tenant.getApartmentSharing());
        tenant = tenantService.updateTenantStatus(tenant);
        return tenantMapper.toTenantModel(tenantRepository.save(tenant));
    }
}
