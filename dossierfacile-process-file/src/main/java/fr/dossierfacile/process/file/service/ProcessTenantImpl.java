package fr.dossierfacile.process.file.service;

import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.type.TaxDocument;
import fr.dossierfacile.process.file.repository.DocumentRepository;
import fr.dossierfacile.process.file.repository.TenantRepository;
import fr.dossierfacile.process.file.service.interfaces.ProcessTaxDocument;
import fr.dossierfacile.process.file.service.interfaces.ProcessTenant;
import java.util.ArrayList;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProcessTenantImpl implements ProcessTenant {

    private final TenantRepository tenantRepository;
    private final ProcessTaxDocument processTaxDocument;
    private final DocumentRepository documentRepository;

    @Override
    public void process(Long tenantId) {
        tenantRepository.findByIdAndFirstNameIsNotNullAndLastNameIsNotNull(tenantId)
                .ifPresent(tenant -> Optional.ofNullable(tenant.getDocuments())
                        .orElse(new ArrayList<>())
                        .stream()
                        .filter(d -> d.getDocumentCategory() == DocumentCategory.TAX)
                        .filter(d -> !d.getNoDocument())
                        .forEach(document -> {
                            if (!tenant.getFirstName().isBlank() && !tenant.getLastName().isBlank()) {
                                TaxDocument taxDocument = processTaxDocument.process(document, tenant);
                                document.setTaxProcessResult(taxDocument);
                                if (taxDocument.isTaxContentValid()) {
                                    document.setTaxContentValid(Boolean.TRUE);
                                }
                                documentRepository.save(document);
                            }
                        }));
    }
}
