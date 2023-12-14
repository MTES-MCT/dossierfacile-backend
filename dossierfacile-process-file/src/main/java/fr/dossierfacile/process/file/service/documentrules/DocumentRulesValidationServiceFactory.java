package fr.dossierfacile.process.file.service.documentrules;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.enums.DocumentCategory;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import static fr.dossierfacile.common.enums.DocumentSubCategory.CERTIFICATE_VISA;

@Service
@AllArgsConstructor
public class DocumentRulesValidationServiceFactory {
    private final IncomeTaxRulesValidationService incomeTaxRulesValidationService;
    private final GuaranteeProviderRulesValidationService guaranteeProviderRulesValidationService;

    public RulesValidationService get(Document document) {
        if (document.getDocumentCategory() == DocumentCategory.TAX) {
            return incomeTaxRulesValidationService;
        }
        if (document.getDocumentCategory() == DocumentCategory.IDENTIFICATION
                && document.getDocumentSubCategory() == CERTIFICATE_VISA) {
            return guaranteeProviderRulesValidationService;
        }
        return null;
    }
}