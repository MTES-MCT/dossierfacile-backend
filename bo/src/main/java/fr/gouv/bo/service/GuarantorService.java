package fr.gouv.bo.service;

import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.Tenant;
import fr.gouv.bo.exception.GuarantorNotFoundException;
import fr.gouv.bo.repository.GuarantorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class GuarantorService {

    private final GuarantorRepository guarantorRepository;
    private final DocumentService documentService;

    public Tenant deleteById(Long guarantorId) {
        Guarantor guarantor = guarantorRepository.findById(guarantorId).orElseThrow(() -> new GuarantorNotFoundException(guarantorId));
        Tenant tenant = guarantor.getTenant();

        //Removing documents and their files from OVH Storage
        Optional.ofNullable(guarantor.getDocuments())
                .orElse(new ArrayList<>())
                .forEach(documentService::deleteFromStorage);

        guarantorRepository.deleteById(guarantorId);
        tenant.getGuarantors().remove(guarantor);
        return tenant;
    }
}
