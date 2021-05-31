package fr.dossierfacile.api.front.register.tenant;

import fr.dossierfacile.api.front.mapper.TenantMapper;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.register.SaveStep;
import fr.dossierfacile.api.front.register.form.tenant.GuarantorTypeForm;
import fr.dossierfacile.api.front.repository.GuarantorRepository;
import fr.dossierfacile.api.front.repository.TenantRepository;
import fr.dossierfacile.api.front.service.interfaces.DocumentService;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.Tenant;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@AllArgsConstructor
public class GuarantorType implements SaveStep<GuarantorTypeForm> {

    private final TenantRepository tenantRepository;
    private final TenantMapper tenantMapper;
    private final GuarantorRepository guarantorRepository;
    private final DocumentService documentService;

    @Override
    public TenantModel saveStep(Tenant tenant, GuarantorTypeForm guarantorTypeForm) {
        Guarantor guarantor = Guarantor.builder()
                .tenant(tenant)
                .typeGuarantor(guarantorTypeForm.getTypeGuarantor())
                .build();
        guarantorRepository.save(guarantor);
        tenant.getGuarantors().add(guarantor);
        tenant.lastUpdateDateProfile(LocalDateTime.now(), null);
        documentService.updateOthersDocumentsStatus(tenant);
        return tenantMapper.toTenantModel(tenantRepository.save(tenant));
    }
}
