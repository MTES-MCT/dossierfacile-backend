package fr.dossierfacile.process.file.service;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.type.TaxDocument;
import fr.dossierfacile.process.file.amqp.model.TenantModel;
import fr.dossierfacile.process.file.exception.TenantNotFoundException;
import fr.dossierfacile.process.file.repository.DocumentRepository;
import fr.dossierfacile.process.file.repository.TenantRepository;
import fr.dossierfacile.process.file.service.interfaces.ProcessTaxDocument;
import fr.dossierfacile.process.file.service.interfaces.ProcessTenant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProcessTenantImpl implements ProcessTenant {

    private final TenantRepository tenantRepository;
    private final ProcessTaxDocument processTaxDocument;
    private final DocumentRepository documentRepository;

    @Override
    public void process(TenantModel tenantModel) {
        Tenant tenant = tenantRepository.findById(tenantModel.getId())
                .orElseThrow(() -> new TenantNotFoundException(tenantModel.getId()));
        List<Document> documents = tenant.getDocuments();
        documents.stream().filter(d -> d.getDocumentCategory() == DocumentCategory.TAX)
                .filter(d -> !d.getNoDocument())
                .forEach(document -> {
                    TaxDocument taxDocument = processTaxDocument.process(document, tenant);
                    document.setTaxProcessResult(taxDocument);
                    documentRepository.save(document);
                });
    }
}
