package fr.dossierfacile.document.analysis.rule.validator.tax;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentAnalysisRule;
import fr.dossierfacile.common.entity.DocumentIAFileAnalysis;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.common.entity.rule.NamesRuleData;
import fr.dossierfacile.common.model.document_ia.BarcodeModel;
import fr.dossierfacile.common.model.document_ia.GenericProperty;
import fr.dossierfacile.document.analysis.rule.validator.RuleValidatorOutput;
import fr.dossierfacile.document.analysis.rule.validator.tax.document_ia_model.TaxName;
import fr.dossierfacile.document.analysis.util.NameUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TaxNamesRule extends BaseTaxRule {

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
        return DocumentRule.R_TAX_NAMES;
    }

    @Override
    public RuleValidatorOutput validate(Document document) {
        var documentIAAnalyses = this.getSuccessfulDocumentIAAnalyses(document);

        var nameToMatch = getNamesFromDocument(document);

        NamesRuleData namesRuleData = null;
        if (nameToMatch != null) {
            var expectedName = new NamesRuleData.Name(
                    nameToMatch.getFirstNamesAsString(),
                    nameToMatch.getLastName(),
                    nameToMatch.getPreferredName()
            );

            namesRuleData = new NamesRuleData(expectedName, List.of());
        }

        var tax = documentIAAnalyses.stream()
                .map(DocumentIAFileAnalysis::getResult)
                .flatMap(result -> result.getBarcodes().stream())
                .filter(this::isTax)
                .toList();

        if (tax.isEmpty() || nameToMatch == null) {
            return new RuleValidatorOutput(false, isBlocking(), DocumentAnalysisRule.documentInconclusiveRuleFromWithData(getRule(), namesRuleData), RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
        }

        var isNameMatch = false;

        var listOfExtractedNames = new ArrayList<NamesRuleData.Name>();
        for (BarcodeModel barcodeModel : tax) {
            List<TaxName> barcodeTaxNames = convertBarcodeModelToTaxName(barcodeModel);
            for (TaxName name : barcodeTaxNames) {
                if (NameUtil.isNameMatching(nameToMatch, name)) {
                    isNameMatch = true;
                }
                listOfExtractedNames.add(
                        new NamesRuleData.Name(
                                String.join(" ", name.getFirstNames()),
                                name.getLastName(),
                                null
                        )
                );
            }
        }

        namesRuleData = new NamesRuleData(
                namesRuleData,
                listOfExtractedNames
        );

        if (isNameMatch) {
            return new RuleValidatorOutput(true, isBlocking(), DocumentAnalysisRule.documentPassedRuleFromWithData(getRule(), namesRuleData), RuleValidatorOutput.RuleLevel.PASSED);
        } else {
            return new RuleValidatorOutput(false, isBlocking(), DocumentAnalysisRule.documentFailedRuleFromWithData(getRule(), namesRuleData), RuleValidatorOutput.RuleLevel.FAILED);
        }
    }

    @Override
    protected boolean isValid(Document document) {
        return false;
    }

    private List<TaxName> convertBarcodeModelToTaxName(BarcodeModel barcodeModel) {
        var declarant1Name = barcodeModel.getTypedData().stream().filter(data -> data.getName().equals("declarant_1")).findFirst();
        var declarant2Name = barcodeModel.getTypedData().stream().filter(data -> data.getName().equals("declarant_2")).findFirst();

        if (declarant2Name.isEmpty() && declarant1Name.isEmpty()) {
            return List.of();
        }

        var taxNames = new ArrayList<TaxName>();
        declarant1Name.flatMap(this::convertGenericProperty).ifPresent(taxNames::add);
        declarant2Name.flatMap(this::convertGenericProperty).ifPresent(taxNames::add);

        return taxNames;
    }

    private Optional<TaxName> convertGenericProperty(GenericProperty genericProperty) {
        var string = genericProperty.getStringValue();
        if (string != null) {
            var parts = string.split(" ");
            var lastName = parts[0];
            var firstNames = List.of(parts).subList(1, parts.length);
            return Optional.of(new TaxName(firstNames, lastName));
        }
        return Optional.empty();
    }


}
