package fr.dossierfacile.document.analysis.rule.validator.payslip;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentAnalysisRule;
import fr.dossierfacile.common.entity.DocumentIAFileAnalysis;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.common.entity.rule.PayslipClassificationRuleData;
import fr.dossierfacile.document.analysis.rule.validator.RuleValidatorOutput;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.BaseDocumentIAValidator;

import java.time.Clock;
import java.util.ArrayList;

public class PayslipClassificationValidatorB extends BasePayslipRuleValidator {

	private static final String EXPECTED_DOCUMENT_TYPE = "bulletin_salaire";

	public PayslipClassificationValidatorB() {
		super(Clock.systemDefaultZone());
	}

	@Override
	protected boolean isBlocking() {
		return true;
	}

	@Override
	protected boolean isInconclusive() {
		return false;
	}

	@Override
	protected DocumentRule getRule() {
		return DocumentRule.R_DOCUMENT_IA_CLASSIFICATION;
	}

	@Override
	public RuleValidatorOutput validate(Document document) {
		var documentIAAnalyses = this.getSuccessfulDocumentIAAnalyses(document);
		var ruleData = new PayslipClassificationRuleData(new ArrayList<>(), getExpectedMonthsLists());

		if (documentIAAnalyses.isEmpty()) {
			return new RuleValidatorOutput(false, isBlocking(), DocumentAnalysisRule.documentFailedRuleFromWithData(getRule(), ruleData), RuleValidatorOutput.RuleLevel.FAILED);
		}

		for (DocumentIAFileAnalysis analysis : documentIAAnalyses) {
			String documentType = null;
			if (analysis.getResult() != null && analysis.getResult().getClassification() != null) {
				documentType = analysis.getResult().getClassification().getDocumentType();
			}

			if (!EXPECTED_DOCUMENT_TYPE.equals(documentType)) {
				ruleData = ruleData.addItem(
						new PayslipClassificationRuleData.PayslipClassificationEntry(
								getFileId(analysis),
								getFileName(analysis)
						)
				);
			}
		}

		if (ruleData.entriesInError().isEmpty()) {
			return new RuleValidatorOutput(true, isBlocking(), DocumentAnalysisRule.documentPassedRuleFromWithData(getRule(), ruleData), RuleValidatorOutput.RuleLevel.PASSED);
		}

		return new RuleValidatorOutput(false, isBlocking(), DocumentAnalysisRule.documentFailedRuleFromWithData(getRule(), ruleData), RuleValidatorOutput.RuleLevel.FAILED);
	}

	private String getFileName(DocumentIAFileAnalysis analysis) {
		if (analysis.getFile() == null || analysis.getFile().getStorageFile() == null) {
			return null;
		}
		return analysis.getFile().getStorageFile().getName();
	}

	private Long getFileId(DocumentIAFileAnalysis analysis) {
		if (analysis.getFile() == null) {
			return null;
		}
		return analysis.getFile().getId();
	}

	@Override
	protected boolean isValid(Document document) {
		return false;
	}
}

