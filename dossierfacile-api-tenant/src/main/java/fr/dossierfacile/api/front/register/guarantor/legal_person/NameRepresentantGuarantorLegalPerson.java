package fr.dossierfacile.api.front.register.guarantor.legal_person;

import fr.dossierfacile.api.front.exception.GuarantorNotFoundException;
import fr.dossierfacile.api.front.mapper.TenantMapper;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.register.SaveStep;
import fr.dossierfacile.api.front.register.form.guarantor.legal_person.NameGuarantorRepresentantLegalPersonForm;
import fr.dossierfacile.api.front.repository.GuarantorRepository;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.TypeGuarantor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NameRepresentantGuarantorLegalPerson implements SaveStep<NameGuarantorRepresentantLegalPersonForm> {

    private final TenantMapper tenantMapper;
    private final GuarantorRepository guarantorRepository;

    @Override
    public TenantModel saveStep(Tenant tenant, NameGuarantorRepresentantLegalPersonForm nameGuarantorRepresentantLegalPersonForm) {
        Guarantor guarantor = guarantorRepository.findByTenantAndTypeGuarantorAndId(tenant, TypeGuarantor.LEGAL_PERSON, nameGuarantorRepresentantLegalPersonForm.getGuarantorId())
                .orElseThrow(() -> new GuarantorNotFoundException(nameGuarantorRepresentantLegalPersonForm.getGuarantorId()));
        guarantor.setFirstName(nameGuarantorRepresentantLegalPersonForm.getFirstName());
        guarantor.setTenant(tenant);
        guarantorRepository.save(guarantor);

        return tenantMapper.toTenantModel(tenant);
    }

}
