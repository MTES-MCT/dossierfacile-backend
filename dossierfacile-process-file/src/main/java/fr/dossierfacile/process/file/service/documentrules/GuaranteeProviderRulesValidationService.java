package fr.dossierfacile.process.file.service.documentrules;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.process.file.service.documentrules.validator.AbstractDocumentRuleValidator;
import fr.dossierfacile.process.file.service.documentrules.validator.guaranteeProvider.GuaranteeProviderHasBeenParsed;
import fr.dossierfacile.process.file.service.documentrules.validator.guaranteeProvider.GuaranteeProviderNamesMatch;
import fr.dossierfacile.process.file.service.documentrules.validator.guaranteeProvider.GuaranteeProviderRuleYearValidity;
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
                new GuaranteeProviderHasBeenParsed(),
                new GuaranteeProviderNamesMatch(),
                new GuaranteeProviderRuleYearValidity()
        );
    }
}