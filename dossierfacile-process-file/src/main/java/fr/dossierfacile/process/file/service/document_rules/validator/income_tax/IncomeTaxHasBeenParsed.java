package fr.dossierfacile.process.file.service.document_rules.validator.income_tax;

import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.entity.ocr.TaxIncomeMainFile;
import fr.dossierfacile.common.enums.ParsedFileAnalysisStatus;
import fr.dossierfacile.common.enums.ParsedFileClassification;
import fr.dossierfacile.process.file.service.document_rules.validator.AbstractDocumentRuleValidator;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class IncomeTaxHasBeenParsed extends AbstractDocumentRuleValidator {

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
        return DocumentRule.R_TAX_PARSE;
    }

    @Override
    protected boolean isValid(Document document) {

        for (File dfFile : document.getFiles()) {
            ParsedFileAnalysis analysis = dfFile.getParsedFileAnalysis();
            if (analysis == null || analysis.getAnalysisStatus() == ParsedFileAnalysisStatus.FAILED) {
                break;
            }
            if (analysis.getClassification() == ParsedFileClassification.TAX_INCOME) {
                TaxIncomeMainFile parsedDocument = (TaxIncomeMainFile) analysis.getParsedFile();
                if (parsedDocument != null
                        && parsedDocument.getAnneeDesRevenus() != null
                        && parsedDocument.getDeclarant1Nom() != null
                        && parsedDocument.getRevenuFiscalDeReference() != null) {
                    return true;
                }
            }
        }
        return false;

    }
}