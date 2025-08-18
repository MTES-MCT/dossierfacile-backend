package fr.dossierfacile.process.file.service.documentrules.validator.incomeTax;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.ParsedFileAnalysis;
import fr.dossierfacile.common.entity.ocr.TaxIncomeLeaf;
import fr.dossierfacile.common.entity.ocr.TaxIncomeMainFile;
import fr.dossierfacile.common.enums.ParsedFileAnalysisStatus;
import fr.dossierfacile.common.enums.ParsedFileClassification;
import fr.dossierfacile.process.file.service.documentrules.validator.AbstractDocumentRuleValidator;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class IncomeTaxRuleAllTaxLeafPresent extends AbstractDocumentRuleValidator {

    @Override
    protected boolean isBlocking() {
        return false;
    }

    @Override
    protected boolean isInconclusive() {
        return false;
    }

    @Override
    protected DocumentRule getRule() {
        return DocumentRule.R_TAX_LEAF;
    }

    @Override
    protected boolean isValid(Document document) {
        // missing pages - currently only check when page are linked in mainFile
        // TODO later - rebuild the entire document from external taxincome leaf
        for (File dfFile : document.getFiles()) {
            ParsedFileAnalysis analysis = dfFile.getParsedFileAnalysis();
            if (analysis == null || analysis.getAnalysisStatus() == ParsedFileAnalysisStatus.FAILED) {
                return false;
            }
            if (analysis.getClassification() == ParsedFileClassification.TAX_INCOME) {
                TaxIncomeMainFile parsedDocument = (TaxIncomeMainFile) analysis.getParsedFile();
                TaxIncomeLeaf leaf = parsedDocument.getTaxIncomeLeaves().getFirst();
                if (leaf != null && leaf.getPageCount() != null && leaf.getPageCount() > parsedDocument.getTaxIncomeLeaves().size()) {
                    return false;
                }
            }
        }
        return true;

    }
}