package fr.dossierfacile.api.front.service.document.analysis.rule;

import fr.dossierfacile.api.front.service.document.analysis.rule.validator.AbstractDocumentRuleValidator;
import fr.dossierfacile.api.front.service.document.analysis.rule.validator.RuleValidatorOutput;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentAnalysisReport;
import fr.dossierfacile.common.entity.DocumentAnalysisStatus;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public abstract class AbstractRulesValidationService {

    abstract List<AbstractDocumentRuleValidator> getDocumentRuleValidators(Document document);

    public DocumentAnalysisReport process(Document document, DocumentAnalysisReport report) {
        try {
            for (var ruleValidator : getDocumentRuleValidators(document)) {
                var output = ruleValidator.validate(document);
                switch (output.ruleLevel()) {
                    case PASSED -> report.addDocumentPassedRule(output.rule());
                    case FAILED -> report.addDocumentFailedRule(output.rule());
                    case INCONCLUSIVE -> report.addDocumentInconclusiveRule(output.rule());
                }
                if (output.isBlocking() && output.ruleLevel() != RuleValidatorOutput.RuleLevel.PASSED) {
                    return report;
                }
            }
        } catch (Exception e) {
            log.error("Error during the rules validation execution process", e);
            report.setAnalysisStatus(DocumentAnalysisStatus.UNDEFINED);
        }
        return report;
    }
}