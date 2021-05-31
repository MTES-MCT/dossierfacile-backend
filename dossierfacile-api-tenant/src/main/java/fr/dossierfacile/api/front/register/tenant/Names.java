package fr.dossierfacile.api.front.register.tenant;

import fr.dossierfacile.api.front.mapper.TenantMapper;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.register.SaveStep;
import fr.dossierfacile.api.front.register.form.tenant.NamesForm;
import fr.dossierfacile.api.front.repository.TenantRepository;
import fr.dossierfacile.api.front.service.interfaces.DocumentService;
import fr.dossierfacile.common.entity.Tenant;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@AllArgsConstructor
public class Names implements SaveStep<NamesForm> {

    private final TenantRepository tenantRepository;
    private final TenantMapper tenantMapper;
    private final DocumentService documentService;

    @Override
    public TenantModel saveStep(Tenant tenant, NamesForm namesForm) {
        tenant.setFirstName(namesForm.getFirstName());
        tenant.setLastName(namesForm.getLastName());
        tenant.setZipCode(namesForm.getZipCode());
        tenant.lastUpdateDateProfile(LocalDateTime.now(), null);
        documentService.updateOthersDocumentsStatus(tenant);
        return tenantMapper.toTenantModel(tenantRepository.save(tenant));
    }
}
