package fr.dossierfacile.process.file.service.document_rules;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentAnalysisReport;
import fr.dossierfacile.common.entity.DocumentAnalysisStatus;
import fr.dossierfacile.process.file.service.document_rules.validator.AbstractDocumentRuleValidator;
import fr.dossierfacile.process.file.service.document_rules.validator.RuleValidatorOutput;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public abstract class AbstractRulesValidationService {
    abstract boolean shouldBeApplied(Document document);

    abstract List<AbstractDocumentRuleValidator> getDocumentRuleValidators();

    public DocumentAnalysisReport process(Document document, DocumentAnalysisReport report) {
        try {
            for (var ruleValidator : getDocumentRuleValidators()) {
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