package fr.dossierfacile.document.analysis.rule.validator.property_tax;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentAnalysisRule;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.common.entity.rule.TaxYearsRuleData;
import fr.dossierfacile.document.analysis.rule.validator.RuleValidatorOutput;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.BaseDocumentIAValidator;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.mapper.DocumentIAMergerMapper;
import fr.dossierfacile.document.analysis.rule.validator.property_tax.document_ia_model.PropertyTaxModel;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;

/*
 * Rule R_PROPERTY_TAX_WRONG_YEAR:
 *
 * Checks the year (annee_imposition) of the property tax notice (taxe foncière).
 *
 * Acceptable years depending on the date:
 * - Before August 1st: N-1
 * - Between August 1st and October 1st: N-1 and N
 * - From October 1st: N
 *
 * Decisions:
 * - The rule is blocking. The year is mandatory: if it is not extracted, the document is refused
 *   (FAILED) — see the product decision "missing data -> refusal".
 * - A present but unparseable value yields INCONCLUSIVE (cannot conclude), mirroring TaxYearRule.
 */
public class PropertyTaxYearRule extends BaseDocumentIAValidator {

    private final Clock clock;

    public PropertyTaxYearRule() {
        this(Clock.systemDefaultZone());
    }

    public PropertyTaxYearRule(Clock clock) {
        this.clock = clock;
    }

    @Override
    protected boolean isBlocking() {
        return true;
    }

    @Override
    protected boolean isInconclusive() {
        return true;
    }

    @Override
    protected DocumentRule getRule() {
        return DocumentRule.R_PROPERTY_TAX_WRONG_YEAR;
    }

    @Override
    public RuleValidatorOutput validate(Document document) {
        var documentIAAnalyses = this.getSuccessfulDocumentIAAnalyses(document);
        var expectedYears = getExpectedYears();

        var extractedYearString = new DocumentIAMergerMapper()
                .map(documentIAAnalyses, PropertyTaxModel.class)
                .map(model -> model.anneeImposition)
                .filter(year -> !year.isBlank())
                .orElse(null);

        var yearRuleData = new TaxYearsRuleData(expectedYears.getFirst(), List.of());

        // The year was not extracted: refused (decision "missing data -> refusal").
        if (extractedYearString == null) {
            return new RuleValidatorOutput(false, isBlocking(), DocumentAnalysisRule.documentFailedRuleFromWithData(getRule(), yearRuleData), RuleValidatorOutput.RuleLevel.FAILED);
        }

        try {
            var extractedYear = Integer.parseInt(extractedYearString.trim());
            yearRuleData = new TaxYearsRuleData(yearRuleData, List.of(extractedYear));

            if (expectedYears.contains(extractedYear)) {
                return new RuleValidatorOutput(true, isBlocking(), DocumentAnalysisRule.documentPassedRuleFromWithData(getRule(), yearRuleData), RuleValidatorOutput.RuleLevel.PASSED);
            }
            return new RuleValidatorOutput(false, isBlocking(), DocumentAnalysisRule.documentFailedRuleFromWithData(getRule(), yearRuleData), RuleValidatorOutput.RuleLevel.FAILED);
        } catch (NumberFormatException e) {
            // Present but unparseable: cannot conclude.
            return new RuleValidatorOutput(false, isBlocking(), DocumentAnalysisRule.documentInconclusiveRuleFromWithData(getRule(), yearRuleData), RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
        }
    }

    @Override
    protected boolean isValid(Document document) {
        return false;
    }

    private List<Integer> getExpectedYears() {
        LocalDate now = LocalDate.now(clock);
        int currentYear = now.getYear();
        LocalDate august1 = LocalDate.of(currentYear, Month.AUGUST, 1);
        LocalDate october1 = LocalDate.of(currentYear, Month.OCTOBER, 1);

        if (now.isBefore(august1)) {
            return List.of(currentYear - 1);
        } else if (now.isBefore(october1)) {
            return List.of(currentYear - 1, currentYear);
        } else {
            return List.of(currentYear);
        }
    }
}
