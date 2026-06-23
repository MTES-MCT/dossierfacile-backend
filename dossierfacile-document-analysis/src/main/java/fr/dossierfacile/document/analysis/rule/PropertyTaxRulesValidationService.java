package fr.dossierfacile.document.analysis.rule;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.document.analysis.rule.validator.AbstractDocumentRuleValidator;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.ClassificationValidatorB;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.HasBeenDocumentIAAnalysedBI;
import fr.dossierfacile.document.analysis.rule.validator.property_tax.PropertyTaxLeafRule;
import fr.dossierfacile.document.analysis.rule.validator.property_tax.PropertyTaxNamesRule;
import fr.dossierfacile.document.analysis.rule.validator.property_tax.PropertyTaxYearRule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

// Dedicated validation service for the property tax notice (taxe foncière, document category RESIDENCY/OWNER).
// Single-file document with strict "taxe_fonciere" classification.
@Service
@RequiredArgsConstructor
@Slf4j
public class PropertyTaxRulesValidationService extends AbstractRulesValidationService {

    @Override
    List<AbstractDocumentRuleValidator> getDocumentRuleValidators(Document document) {
        return List.of(
                new HasBeenDocumentIAAnalysedBI(),
                new ClassificationValidatorB("taxe_fonciere"),
                new PropertyTaxLeafRule(),
                new PropertyTaxNamesRule(),
                new PropertyTaxYearRule()
        );
    }
}
