package fr.dossierfacile.api.front.register.tenant;

import fr.dossierfacile.api.front.mapper.TenantMapper;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.register.SaveStep;
import fr.dossierfacile.api.front.register.form.tenant.GuarantorTypeForm;
import fr.dossierfacile.api.front.repository.GuarantorRepository;
import fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@AllArgsConstructor
public class GuarantorType implements SaveStep<GuarantorTypeForm> {

    private final TenantCommonRepository tenantRepository;
    private final TenantMapper tenantMapper;
    private final GuarantorRepository guarantorRepository;
    private final TenantService tenantService;
    private final ApartmentSharingService apartmentSharingService;

    @Override
    @Transactional
    public TenantModel saveStep(Tenant tenant, GuarantorTypeForm guarantorTypeForm) {
        Guarantor guarantor = Guarantor.builder()
                .tenant(tenant)
                .typeGuarantor(guarantorTypeForm.getTypeGuarantor())
                .build();
        guarantorRepository.save(guarantor);
        tenant.getGuarantors().add(guarantor);
        tenant.lastUpdateDateProfile(LocalDateTime.now(), null);
        tenantService.updateTenantStatus(tenant);
        apartmentSharingService.resetDossierPdfGenerated(tenant.getApartmentSharing());
        return tenantMapper.toTenantModel(tenantRepository.save(tenant));
    }
}
