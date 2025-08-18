package fr.dossierfacile.process.file.service.document_rules;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.process.file.service.document_rules.validator.AbstractDocumentRuleValidator;
import fr.dossierfacile.process.file.service.document_rules.validator.income_tax.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class IncomeTaxRulesValidationService extends AbstractRulesValidationService {
    @Override
    public boolean shouldBeApplied(Document document) {
        return document.getDocumentCategory() == DocumentCategory.TAX
                && document.getDocumentSubCategory() == DocumentSubCategory.MY_NAME
                && !CollectionUtils.isEmpty(document.getFiles())
                && document.getFiles().stream().anyMatch((f) -> f.getFileAnalysis() != null);
    }

    @Override
    List<AbstractDocumentRuleValidator> getDocumentRuleValidators() {
        return List.of(
                new IncomeTaxHas2DDoc(),
                new IncomeTaxHasGoodClassification(),
                new IncomeTaxHasBeenParsed(),
                new IncomeTaxRuleConsistency(),
                new IncomeTaxRuleCheckNMinus1(),
                new IncomeTaxRuleCheckNMinus3(),
                new IncomeTaxRuleNamesMatch(),
                new IncomeTaxRuleTaxLeafPresent(),
                new IncomeTaxRuleAllTaxLeafPresent()
        );
    }
}