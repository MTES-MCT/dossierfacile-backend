package fr.dossierfacile.process.file.service;

import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.TypeGuarantor;
import fr.dossierfacile.process.file.repository.TenantRepository;
import fr.dossierfacile.process.file.service.interfaces.DocumentService;
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
    private final DocumentService documentService;

    @Override
    public void process(Long tenantId) {
        tenantRepository.findByIdAndFirstNameIsNotNullAndLastNameIsNotNull(tenantId)
                .ifPresent(tenant -> {
                    if (!tenant.getFirstName().isBlank() && !tenant.getLastName().isBlank()) {
                        Optional.ofNullable(tenant.getDocuments())
                                .orElse(new ArrayList<>())
                                .stream()
                                .filter(d -> d.getDocumentCategory() == DocumentCategory.TAX)
                                .filter(d -> !d.getNoDocument())
                                .forEach(document -> documentService.updateTaxProcessResult(processTaxDocument.process(document, tenant), document.getId()));
                        Optional.ofNullable(tenant.getGuarantors())
                                .orElse(new ArrayList<>())
                                .stream()
                                .filter(g -> g.getTypeGuarantor() == TypeGuarantor.NATURAL_PERSON)
                                .forEach(guarantor -> {
                                    Optional.ofNullable(guarantor.getDocuments())
                                            .orElse(new ArrayList<>())
                                            .stream()
                                            .filter(d -> d.getDocumentCategory() == DocumentCategory.TAX)
                                            .filter(d -> !d.getNoDocument())
                                            .forEach(document -> documentService.updateTaxProcessResult(processTaxDocument.process(document, tenant), document.getId()));
                                });
                    }
                });
    }
}
