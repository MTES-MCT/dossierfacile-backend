package fr.dossierfacile.document.analysis.rule.validator.tax;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentAnalysisRule;
import fr.dossierfacile.common.entity.DocumentIAFileAnalysis;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.common.model.document_ia.GenericProperty;
import fr.dossierfacile.document.analysis.rule.validator.RuleValidatorOutput;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.BaseDocumentIAValidator;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;

public class TaxYearRule extends BaseTaxRule {

    private final Clock clock;

    public TaxYearRule() {
        this(Clock.systemDefaultZone());
    }

    public TaxYearRule(Clock clock) {
        this.clock = clock;
    }


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
        return DocumentRule.R_TAX_WRONG_YEAR;
    }

    @Override
    public RuleValidatorOutput validate(Document document) {
        var documentIAAnalyses = this.getSuccessfulDocumentIAAnalyses(document);

        var expectedYear = getTaxYear();

        var tax = documentIAAnalyses.stream()
                .map(DocumentIAFileAnalysis::getResult)
                .flatMap(result -> result.getBarcodes().stream())
                .filter(this::isTax)
                .toList();

        var expectedDatas = List.of(
                new GenericProperty("expected_year", expectedYear, "number")
        );

        if (tax.isEmpty()) {
            return new RuleValidatorOutput(false, isBlocking(), DocumentAnalysisRule.documentInconclusiveRuleFromWithData(getRule(), expectedDatas), RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
        }

        var listOfPresentYear = tax.stream().flatMap(it -> it.getTypedData().stream())
                .filter(genericProperty -> genericProperty.getName().equals("annee_des_revenus"))
                .map(GenericProperty::getValue)
                .toList();

        if (listOfPresentYear.isEmpty()) {
            return new RuleValidatorOutput(false, isBlocking(), DocumentAnalysisRule.documentInconclusiveRuleFromWithData(getRule(), expectedDatas), RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
        }

        var foundsData = listOfPresentYear.stream().map(
                it -> new GenericProperty("found_year", it, "number")
        ).toList();

        if (listOfPresentYear.contains(expectedYear)) {
            return new RuleValidatorOutput(true, isBlocking(), DocumentAnalysisRule.documentPassedRuleFromWithData(getRule(), expectedDatas, foundsData), RuleValidatorOutput.RuleLevel.PASSED);
        } else {
            return new RuleValidatorOutput(false, isBlocking(), DocumentAnalysisRule.documentFailedRuleFromWithData(getRule(), expectedDatas, foundsData), RuleValidatorOutput.RuleLevel.FAILED);
        }
    }

    @Override
    protected boolean isValid(Document document) {
        return false;
    }

    private int getTaxYear() {
        LocalDate now = LocalDate.now(clock);
        int currentYear = now.getYear();
        LocalDate dateOfChange = LocalDate.of(currentYear, Month.SEPTEMBER, 15);

        return now.isBefore(dateOfChange) ? currentYear - 2 : currentYear - 1;
    }
}
