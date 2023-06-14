package fr.gouv.bo.service;

import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.Tenant;
import fr.gouv.bo.exception.GuarantorNotFoundException;
import fr.gouv.bo.repository.GuarantorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class GuarantorService {
    private final GuarantorRepository guarantorRepository;

    public Tenant deleteById(Long guarantorId) {
        Guarantor guarantor = guarantorRepository.findById(guarantorId).orElseThrow(() -> new GuarantorNotFoundException(guarantorId));
        Tenant tenant = guarantor.getTenant();

        guarantorRepository.deleteById(guarantorId);
        tenant.getGuarantors().remove(guarantor);
        return tenant;
    }
}
