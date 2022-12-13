package fr.dossierfacile.process.file.service;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.TypeGuarantor;
import fr.dossierfacile.common.type.TaxDocument;
import fr.dossierfacile.process.file.repository.TenantRepository;
import fr.dossierfacile.process.file.service.interfaces.DocumentService;
import fr.dossierfacile.process.file.service.interfaces.ProcessTaxDocument;
import fr.dossierfacile.process.file.service.interfaces.ProcessTenant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

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
                .filter(tenant -> isNotBlank(tenant.getFirstName()) && isNotBlank(tenant.getLastName()))
                .ifPresent(tenant -> {
                    processDocumentsOf(tenant);
                    getGuarantorPersonsOf(tenant).forEach(this::processDocumentsOf);
                });
    }

    private void processDocumentsOf(Tenant tenant) {
        getTaxDocuments(tenant.getDocuments())
                .forEach(document -> {
                    TaxDocument result = processTaxDocument.process(document, tenant);
                    documentService.updateTaxProcessResult(result, document.getId());
                });
    }

    private void processDocumentsOf(Guarantor guarantor) {
        getTaxDocuments(guarantor.getDocuments())
                .forEach(document -> {
                    TaxDocument result = processTaxDocument.process(document, guarantor);
                    documentService.updateTaxProcessResult(result, document.getId());
                });
    }

    private Stream<Document> getTaxDocuments(List<Document> tenant) {
        return Optional.ofNullable(tenant)
                .orElse(new ArrayList<>())
                .stream()
                .filter(d -> d.getDocumentCategory() == DocumentCategory.TAX)
                .filter(d -> !d.getNoDocument());
    }

    private Stream<Guarantor> getGuarantorPersonsOf(Tenant tenant) {
        return Optional.ofNullable(tenant.getGuarantors())
                .orElse(new ArrayList<>())
                .stream()
                .filter(g -> g.getTypeGuarantor() == TypeGuarantor.NATURAL_PERSON);
    }
}
