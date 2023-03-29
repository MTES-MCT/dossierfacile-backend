package fr.dossierfacile.process.file.service;

import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.TypeGuarantor;
import fr.dossierfacile.common.type.TaxDocument;
import fr.dossierfacile.process.file.repository.TenantRepository;
import fr.dossierfacile.process.file.service.interfaces.DocumentService;
import fr.dossierfacile.process.file.service.interfaces.ProcessTaxDocument;
import fr.dossierfacile.process.file.service.interfaces.ProcessTenant;
import fr.dossierfacile.process.file.service.qrcodeanalysis.QrCodeFileProcessor;
import fr.dossierfacile.process.file.util.Documents;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Stream;

import static org.apache.commons.lang3.BooleanUtils.isNotTrue;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProcessTenantImpl implements ProcessTenant {

    private final TenantRepository tenantRepository;
    private final ProcessTaxDocument processTaxDocument;
    private final DocumentService documentService;
    private final QrCodeFileProcessor qrCodeDocumentsProcessor;

    @Override
    public void process(Long tenantId) {
        tenantRepository.findByIdAndFirstNameIsNotNullAndLastNameIsNotNull(tenantId)
                .filter(tenant -> isNotBlank(tenant.getFirstName()) && isNotBlank(tenant.getLastName()))
                .ifPresent(tenant -> {
                    processDocumentsWithQrCode(tenant);
                    processTaxDocument(tenant);
                });
    }

    private void processDocumentsWithQrCode(Tenant tenant) {
        Documents documents = Documents.ofTenantAndGuarantors(tenant);
        qrCodeDocumentsProcessor.process(documents);
    }

    private void processTaxDocument(Tenant tenant) {
        if (isNotTrue(tenant.getAllowCheckTax())) {
            log.info("Ignoring tenant {} because they have not allowed automatic tax verification", tenant.getId());
            return;
        }
        Documents.of(tenant).byCategory(DocumentCategory.TAX)
                .forEach(document -> {
                    TaxDocument result = processTaxDocument.process(document, tenant);
                    documentService.updateTaxProcessResult(result, document);
                });
        getGuarantorPersonsOf(tenant).forEach(this::processTaxDocument);
    }

    private void processTaxDocument(Guarantor guarantor) {
        Documents.of(guarantor).byCategory(DocumentCategory.TAX)
                .forEach(document -> {
                    TaxDocument result = processTaxDocument.process(document, guarantor);
                    documentService.updateTaxProcessResult(result, document);
                });
    }

    private Stream<Guarantor> getGuarantorPersonsOf(Tenant tenant) {
        return Optional.ofNullable(tenant.getGuarantors())
                .orElse(new ArrayList<>())
                .stream()
                .filter(g -> g.getTypeGuarantor() == TypeGuarantor.NATURAL_PERSON);
    }

}
