package fr.dossierfacile.api.front.register.guarantor.legal_person;

import fr.dossierfacile.api.front.exception.GuarantorNotFoundException;
import fr.dossierfacile.api.front.mapper.TenantMapper;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.register.SaveStep;
import fr.dossierfacile.api.front.register.form.guarantor.legal_person.NameGuarantorLegalPersonForm;
import fr.dossierfacile.api.front.repository.GuarantorRepository;
import fr.dossierfacile.api.front.security.interfaces.ClientAuthenticationFacade;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.TypeGuarantor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NameGuarantorLegalPerson implements SaveStep<NameGuarantorLegalPersonForm> {

    private final TenantMapper tenantMapper;
    private final GuarantorRepository guarantorRepository;
    private final ClientAuthenticationFacade clientAuthenticationFacade;

    @Override
    public TenantModel saveStep(Tenant tenant, NameGuarantorLegalPersonForm nameGuarantorLegalPersonForm) {
        Guarantor guarantor = guarantorRepository.findByTenantAndTypeGuarantorAndId(tenant, TypeGuarantor.LEGAL_PERSON, nameGuarantorLegalPersonForm.getGuarantorId())
                .orElseThrow(() -> new GuarantorNotFoundException(nameGuarantorLegalPersonForm.getGuarantorId()));
        guarantor.setLegalPersonName(nameGuarantorLegalPersonForm.getLegalPersonName());
        guarantor.setTenant(tenant);
        guarantorRepository.save(guarantor);

        return tenantMapper.toTenantModel(tenant, (!clientAuthenticationFacade.isClient()) ? null : clientAuthenticationFacade.getClient());
    }

}
