package fr.dossierfacile.process.file.service.document_rules;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.process.file.service.document_rules.validator.AbstractDocumentRuleValidator;
import fr.dossierfacile.process.file.service.document_rules.validator.guarantee_provider.GuaranteeProviderHasBeenParsedBI;
import fr.dossierfacile.process.file.service.document_rules.validator.guarantee_provider.GuaranteeProviderNamesMatch;
import fr.dossierfacile.process.file.service.document_rules.validator.guarantee_provider.GuaranteeProviderRuleYearValidity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

import static fr.dossierfacile.common.enums.DocumentSubCategory.OTHER_GUARANTEE;
import static fr.dossierfacile.common.enums.DocumentSubCategory.VISALE;

@Service
@RequiredArgsConstructor
@Slf4j
public class GuaranteeProviderRulesValidationService extends AbstractRulesValidationService {
    @Override
    public boolean shouldBeApplied(Document document) {
        return document.getDocumentCategory() == DocumentCategory.GUARANTEE_PROVIDER_CERTIFICATE
                && List.of(OTHER_GUARANTEE, VISALE).contains(document.getDocumentSubCategory())
                && !CollectionUtils.isEmpty(document.getFiles())
                && document.getFiles().stream().anyMatch((f) -> f.getParsedFileAnalysis() != null
                && f.getParsedFileAnalysis().getParsedFile() != null);
    }

    @Override
    List<AbstractDocumentRuleValidator> getDocumentRuleValidators() {
        return List.of(
                new GuaranteeProviderHasBeenParsedBI(),
                new GuaranteeProviderNamesMatch(),
                new GuaranteeProviderRuleYearValidity()
        );
    }
}