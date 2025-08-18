package fr.dossierfacile.process.file.service.documentrules.validator.incomeTax;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.ParsedFileAnalysis;
import fr.dossierfacile.common.entity.ocr.TaxIncomeMainFile;
import fr.dossierfacile.common.enums.ParsedFileAnalysisStatus;
import fr.dossierfacile.common.enums.ParsedFileClassification;
import fr.dossierfacile.process.file.service.documentrules.validator.AbstractDocumentRuleValidator;
import fr.dossierfacile.process.file.util.PersonNameComparator;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class IncomeTaxRuleConsistency extends AbstractDocumentRuleValidator {

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
        return DocumentRule.R_TAX_FAKE;
    }

    @Override
    protected boolean isValid(Document document) {

        // Fake rule
        for (File dfFile : document.getFiles()) {
            ParsedFileAnalysis analysis = dfFile.getParsedFileAnalysis();
            if (analysis == null || dfFile.getFileAnalysis() == null || analysis.getAnalysisStatus() == ParsedFileAnalysisStatus.FAILED) {
                break;
            }
            if (analysis.getClassification() == ParsedFileClassification.TAX_INCOME) {
                Optional<TaxIncomeMainFile> qrDocument = IncomeTaxHelper.fromQR(dfFile.getFileAnalysis());
                TaxIncomeMainFile parsedDocument = (TaxIncomeMainFile) analysis.getParsedFile();

                if (qrDocument.isEmpty() || parsedDocument == null) {
                    return false;
                }

                var safeQrDocument = qrDocument.get();

                if (parsedDocument.getAnneeDesRevenus() != null
                        && parsedDocument.getDeclarant1Nom() != null
                        && parsedDocument.getRevenuFiscalDeReference() != null
                        && (!safeQrDocument.getAnneeDesRevenus().equals(parsedDocument.getAnneeDesRevenus())
                        || !PersonNameComparator.equalsWithNormalization(safeQrDocument.getDeclarant1Nom(), parsedDocument.getDeclarant1Nom())
                        || !safeQrDocument.getRevenuFiscalDeReference().equals(parsedDocument.getRevenuFiscalDeReference())
                )) {
                    return false;
                }
            }
        }
        return true;

    }
}