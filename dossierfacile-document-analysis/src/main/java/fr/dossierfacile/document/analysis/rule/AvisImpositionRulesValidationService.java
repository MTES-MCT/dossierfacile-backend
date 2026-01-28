package fr.dossierfacile.document.analysis.rule;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.document.analysis.rule.validator.AbstractDocumentRuleValidator;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.HasBeenDocumentIAAnalysedBI;
import fr.dossierfacile.document.analysis.rule.validator.tax.TaxClassificationRuleB;
import fr.dossierfacile.document.analysis.rule.validator.tax.TaxNamesRule;
import fr.dossierfacile.document.analysis.rule.validator.tax.TaxYearRule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AvisImpositionRulesValidationService extends AbstractRulesValidationService {

    @Override
    List<AbstractDocumentRuleValidator> getDocumentRuleValidators(Document document) {
        return List.of(
                new HasBeenDocumentIAAnalysedBI(),
                new TaxClassificationRuleB(),
                new TaxYearRule(),
                new TaxNamesRule()
        );
    }
}