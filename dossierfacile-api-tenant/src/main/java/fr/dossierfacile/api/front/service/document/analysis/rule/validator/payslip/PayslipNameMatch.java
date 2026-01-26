package fr.dossierfacile.api.front.service.document.analysis.rule.validator.payslip;

import fr.dossierfacile.api.front.service.document.analysis.rule.validator.RuleValidatorOutput;
import fr.dossierfacile.api.front.service.document.analysis.rule.validator.document_ia.BaseDocumentIAValidator;
import fr.dossierfacile.api.front.service.document.analysis.rule.validator.document_ia.mapper.DocumentIAMultiMapper;
import fr.dossierfacile.api.front.service.document.analysis.rule.validator.payslip.document_ia_model.PayslipNames;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentAnalysisRule;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.common.model.documentIA.GenericProperty;
import fr.dossierfacile.common.utils.NameUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;

@Slf4j
public class PayslipNameMatch extends BaseDocumentIAValidator {

    @Override
    protected boolean isBlocking() {
        return false;
    }

    @Override
    protected boolean isInconclusive() {
        return true;
    }

    @Override
    protected DocumentRule getRule() {
        return DocumentRule.R_PAYSLIP_NAME_MATCH;
    }

    @Override
    public RuleValidatorOutput validate(Document document) {
        var documentIAAnalyses = this.getSuccessfulDocumentIAAnalyses(document);

        var nameToMatch = getNamesFromDocument(document);
        var expectedDatas = new ArrayList<GenericProperty>();
        if (nameToMatch != null) {
            expectedDatas.add(new GenericProperty("firstNames", nameToMatch.getFirstNamesAsString(), "String"));
            expectedDatas.add(new GenericProperty("lastName", nameToMatch.getLastName(), "String"));
            expectedDatas.add(new GenericProperty("preferredName", nameToMatch.getPreferredName() != null ? nameToMatch.getPreferredName() : "N/A", "String"));
        }

        if (documentIAAnalyses.isEmpty() || hasAnyNonSuccessfulDocumentIAAnalyses(document)) {
            return new RuleValidatorOutput(false, isBlocking(), DocumentAnalysisRule.documentInconclusiveRuleFromWithData(getRule(), expectedDatas), RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
        }

        if (nameToMatch == null) {
            return new RuleValidatorOutput(false, isBlocking(), DocumentAnalysisRule.documentInconclusiveRuleFromWithData(getRule(), expectedDatas), RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
        }

        var isNameMatch = true;

        var extractedIdentities = new DocumentIAMultiMapper().map(documentIAAnalyses, PayslipNames.class);

        var extractedDatas = new ArrayList<GenericProperty>();
        var i = 1;
        for (PayslipNames name : extractedIdentities) {
            if (!NameUtil.isNameMatching(nameToMatch, name)) {
                isNameMatch = false;
            }
            extractedDatas.add(new GenericProperty("document" + i, name.getFirstNames() + " " + name.getLastName(), "String"));
            i++;
        }

        if (isNameMatch) {
            return new RuleValidatorOutput(true, isBlocking(), DocumentAnalysisRule.documentPassedRuleFromWithData(getRule(), expectedDatas, extractedDatas), RuleValidatorOutput.RuleLevel.PASSED);
        } else {
            return new RuleValidatorOutput(false, isBlocking(), DocumentAnalysisRule.documentFailedRuleFromWithData(getRule(), expectedDatas, extractedDatas), RuleValidatorOutput.RuleLevel.FAILED);
        }
    }

    @Override
    protected boolean isValid(Document document) {
        return false;
    }
}
