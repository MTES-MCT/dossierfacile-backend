package fr.dossierfacile.process.file.service.documentrules;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.entity.ocr.TaxIncomeLeaf;
import fr.dossierfacile.common.entity.ocr.TaxIncomeMainFile;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.enums.ParsedFileAnalysisStatus;
import fr.dossierfacile.common.enums.ParsedFileClassification;
import fr.dossierfacile.process.file.barcode.twoddoc.parsing.TwoDDocDataType;
import fr.dossierfacile.process.file.util.PersonNameComparator;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class IncomeTaxRulesValidationService implements RulesValidationService {
    @Override
    public boolean shouldBeApplied(Document document) {
        return document.getDocumentCategory() == DocumentCategory.TAX
                && document.getDocumentSubCategory() == DocumentSubCategory.MY_NAME
                && !CollectionUtils.isEmpty(document.getFiles())
                && document.getFiles().stream().anyMatch((f) -> f.getFileAnalysis() != null);
    }


    private boolean check2DDocPresent(Document document) {
        for (File dfFile : document.getFiles()) {
            BarCodeFileAnalysis barCodeFileAnalysis = dfFile.getFileAnalysis();
            if (barCodeFileAnalysis != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * return false if a 2DDoc is detected AND this 2DDoc indicate
     *
     * @param document
     * @return
     */
    private boolean checkClassificationFrom2DDoc(Document document) {
        for (File dfFile : document.getFiles()) {
            BarCodeFileAnalysis barCodeFileAnalysis = dfFile.getFileAnalysis();
            if (barCodeFileAnalysis == null) {
                break;
            }
            if (barCodeFileAnalysis.getDocumentType() == BarCodeDocumentType.TAX_ASSESSMENT) {
                return true;
            }
        }
        return false;
    }

    private boolean checkDocumentParsed(Document document) {
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

    private TaxIncomeMainFile fromQR(BarCodeFileAnalysis barCodeFileAnalysis) {
        if (barCodeFileAnalysis.getBarCodeType() != BarCodeType.TWO_D_DOC) {
            throw new IllegalStateException("Verified Code has unsupported type");
        }
        Map<String, String> dataWithLabel = new ObjectMapper().convertValue(barCodeFileAnalysis.getVerifiedData(), Map.class);
        return TaxIncomeMainFile.builder()
                .declarant1NumFiscal(dataWithLabel.get(TwoDDocDataType.ID_47.getLabel()))
                .declarant1Nom(dataWithLabel.get(TwoDDocDataType.ID_46.getLabel()))
                .declarant2NumFiscal(dataWithLabel.get(TwoDDocDataType.ID_49.getLabel()))
                .declarant2Nom(dataWithLabel.get(TwoDDocDataType.ID_48.getLabel()))
                .anneeDesRevenus(Integer.parseInt(dataWithLabel.get(TwoDDocDataType.ID_45.getLabel())))
                .nombreDeParts(dataWithLabel.get(TwoDDocDataType.ID_43.getLabel()))
                .dateDeMiseEnRecouvrement(dataWithLabel.get(TwoDDocDataType.ID_4A.getLabel()))
                .revenuFiscalDeReference(Integer.parseInt(dataWithLabel.get(TwoDDocDataType.ID_41.getLabel())))
                .numeroFiscalDeclarant1(dataWithLabel.get(TwoDDocDataType.ID_47.getLabel()))
                .numeroFiscalDeclarant2(dataWithLabel.get(TwoDDocDataType.ID_49.getLabel()))
                .referenceAvis(dataWithLabel.get(TwoDDocDataType.ID_44.getLabel())).build();
    }

    private boolean checkConsistency(Document document) {
        // Fake rule
        for (File dfFile : document.getFiles()) {
            ParsedFileAnalysis analysis = dfFile.getParsedFileAnalysis();
            if (analysis == null || analysis.getAnalysisStatus() == ParsedFileAnalysisStatus.FAILED) {
                break;
            }
            if (analysis.getClassification() == ParsedFileClassification.TAX_INCOME) {
                TaxIncomeMainFile qrDocument = fromQR(dfFile.getFileAnalysis());
                TaxIncomeMainFile parsedDocument = (TaxIncomeMainFile) analysis.getParsedFile();

                if (parsedDocument != null
                        && parsedDocument.getAnneeDesRevenus() != null
                        && parsedDocument.getDeclarant1Nom() != null
                        && parsedDocument.getRevenuFiscalDeReference() != null
                        && (!qrDocument.getAnneeDesRevenus().equals(parsedDocument.getAnneeDesRevenus())
                        || !PersonNameComparator.equalsWithNormalization(qrDocument.getDeclarant1Nom(), parsedDocument.getDeclarant1Nom())
                        || !qrDocument.getRevenuFiscalDeReference().equals(parsedDocument.getRevenuFiscalDeReference())
                )) {
                    return false;
                }
            }
        }
        return true;
    }

    private DocumentAnalysisRule checkNMinus1(Document document, List<Integer> providedYears) {
        // Provide N-1 tax income / not more N-3
        Integer maxYear = providedYears.stream().max(Integer::compare).orElse(null);
        if (maxYear == null) {
            log.info("Tax income has no year :{}", document.getId());
            return DocumentAnalysisRule.documentFailedRuleFrom(DocumentRule.R_TAX_N1);
        }
        // try to check some rules
        LocalDate now = LocalDate.now();
        if (now.isBefore(LocalDate.of(now.getYear(), 9, 15))) {
            if (maxYear != (now.getYear() - 1) && maxYear != (now.getYear() - 2)) {
                return DocumentAnalysisRule.builder()
                        .rule(DocumentRule.R_TAX_N1)
                        .message("L'avis d'impots sur les revenus " + maxYear
                                + ", vous devez fournir un avis d'impots sur les revenus " + (now.getYear() - 1)
                                + " ou " + (now.getYear() - 2))
                        .build();
            }
        } else if (maxYear != (now.getYear() - 1)) {
            return
                    DocumentAnalysisRule.builder()
                            .rule(DocumentRule.R_TAX_N1)
                            .message("L'avis d'impots sur les revenus " + maxYear
                                    + " n'est pas accepté, vous devez fournir un avis d'impots sur les revenus " + (now.getYear() - 1))
                            .build();
        }

        return null;
    }

    private boolean checkNMinus3(List<Integer> providedYears) {
        LocalDate now = LocalDate.now();
        Integer minYear = providedYears.stream().min(Integer::compare).orElse(null);
        int authorisedYear = now.minusMonths(9).minusDays(15).getYear();
        if (minYear == null) {
            return false;
        }
        return minYear >= (authorisedYear - 2);
    }

    private boolean checkNames(Document document) {
        // Firstname LastName PreferredName
        Person documentOwner = Optional.ofNullable((Person) document.getTenant()).orElseGet(document::getGuarantor);
        String firstName = documentOwner.getFirstName();
        String lastName = documentOwner.getLastName();
        String preferredName = documentOwner.getPreferredName();
        for (File dfFile : document.getFiles()) {
            BarCodeFileAnalysis analysis = dfFile.getFileAnalysis();
            if (analysis.getDocumentType() == BarCodeDocumentType.TAX_ASSESSMENT) {
                TaxIncomeMainFile qrDocument = fromQR(dfFile.getFileAnalysis());

                if (!((PersonNameComparator.bearlyEqualsTo(qrDocument.getDeclarant1Nom(), lastName, firstName)
                        || PersonNameComparator.bearlyEqualsTo(qrDocument.getDeclarant1Nom(), preferredName, firstName))
                        || (qrDocument.getDeclarant2Nom() != null &&
                        (PersonNameComparator.bearlyEqualsTo(qrDocument.getDeclarant2Nom(), lastName, firstName)
                                || PersonNameComparator.bearlyEqualsTo(qrDocument.getDeclarant2Nom(), preferredName, firstName)))
                )) {
                    log.info("Le nom/prenom ne correpond pas à l'utilisateur docId:{} firstname: {}", document.getId(), firstName);
                    return false;
                }
            }
        }
        return true;
    }

    private boolean checkIncomeTaxLeafPresent(Document document) {
        // missing pages - currently only check when page are linked in mainFile
        // TODO later - rebuild the entire document from external taxincome leaf
        for (File dfFile : document.getFiles()) {
            ParsedFileAnalysis analysis = dfFile.getParsedFileAnalysis();
            if (analysis == null || analysis.getAnalysisStatus() == ParsedFileAnalysisStatus.FAILED) {
                return false;
            }
            if (analysis.getClassification() == ParsedFileClassification.TAX_INCOME) {
                TaxIncomeMainFile parsedDocument = (TaxIncomeMainFile) analysis.getParsedFile();
                if (CollectionUtils.isEmpty(parsedDocument.getTaxIncomeLeaves())) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean checkAllIncomeTaxLeafPresent(Document document) {
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


    @Override
    @Transactional
    public DocumentAnalysisReport process(Document document, @NotNull DocumentAnalysisReport report) {
        try {

            if (check2DDocPresent(document)) {
                report.addDocumentPassedRule(DocumentAnalysisRule.documentPassedRuleFrom(DocumentRule.R_TAX_2D_DOC));
            } else {
                log.info("Document has no 2DDoc :{}", document.getId());
                report.addDocumentInconclusiveRule(DocumentAnalysisRule.documentInconclusiveRuleFrom(DocumentRule.R_TAX_2D_DOC));
                return report;
            }

            if (checkClassificationFrom2DDoc(document)) {
                report.addDocumentPassedRule(DocumentAnalysisRule.documentPassedRuleFrom(DocumentRule.R_TAX_BAD_CLASSIFICATION));
            } else {
                log.info("Document is not a Tax Assessment :{}", document.getId());
                report.addDocumentFailedRule(DocumentAnalysisRule.documentFailedRuleFrom(DocumentRule.R_TAX_BAD_CLASSIFICATION));
                return report;
            }

            if (checkDocumentParsed(document)) {
                report.addDocumentPassedRule(DocumentAnalysisRule.documentPassedRuleFrom(DocumentRule.R_TAX_PARSE));
            } else {
                log.info("Document has not been correctly parsed :{}", document.getId());
                report.addDocumentInconclusiveRule(DocumentAnalysisRule.documentInconclusiveRuleFrom(DocumentRule.R_TAX_PARSE));
                return report;
            }

            if (checkConsistency(document)) {
                report.addDocumentPassedRule(DocumentAnalysisRule.documentPassedRuleFrom(DocumentRule.R_TAX_FAKE));
            } else {
                log.info("Le 2DDoc code ne correspond pas au contenu du document :{}", document.getId());
                report.addDocumentFailedRule(DocumentAnalysisRule.documentFailedRuleFrom(DocumentRule.R_TAX_FAKE));
                return report;
            }

            List<Integer> providedYears = new ArrayList<>(2);
            for (File dfFile : document.getFiles()) {
                if (dfFile.getFileAnalysis() != null) {
                    TaxIncomeMainFile qrDocument = fromQR(dfFile.getFileAnalysis());
                    if (qrDocument.getAnneeDesRevenus() != null)
                        providedYears.add(qrDocument.getAnneeDesRevenus());
                }
            }

            var brokenRule = checkNMinus1(document, providedYears);
            if (brokenRule != null) {
                log.info("Tax income is deprecated :{}", document.getId());
                report.addDocumentFailedRule(brokenRule);
            } else {
                report.addDocumentPassedRule(DocumentAnalysisRule.documentPassedRuleFrom(DocumentRule.R_TAX_N1));
            }

            if (checkNMinus3(providedYears)) {
                report.addDocumentPassedRule(DocumentAnalysisRule.documentPassedRuleFrom(DocumentRule.R_TAX_N3));
            } else {
                log.info("Tax income is deprecated :{}", document.getId());
                report.addDocumentFailedRule(DocumentAnalysisRule.documentFailedRuleFrom(DocumentRule.R_TAX_N3));
            }

            if (checkNames(document)) {
                report.addDocumentPassedRule(DocumentAnalysisRule.documentPassedRuleFrom(DocumentRule.R_TAX_NAMES));
            } else {
                log.info("Tax income names do not match :{}", document.getId());
                report.addDocumentFailedRule(DocumentAnalysisRule.documentFailedRuleFrom(DocumentRule.R_TAX_NAMES));
            }

            if (checkIncomeTaxLeafPresent(document)) {
                report.addDocumentPassedRule(DocumentAnalysisRule.documentPassedRuleFrom(DocumentRule.R_TAX_LEAF));
            } else {
                log.info("Income tax has not incometaxleaf:{}", document.getId());
                report.addDocumentFailedRule(DocumentAnalysisRule.documentFailedRuleFrom(DocumentRule.R_TAX_LEAF));
                return report;
            }

            if (checkAllIncomeTaxLeafPresent(document)) {
                report.addDocumentPassedRule(DocumentAnalysisRule.documentPassedRuleFrom(DocumentRule.R_TAX_ALL_LEAF));
            } else {
                log.info("Income tax has not ALL incometaxleaf:{}", document.getId());
                report.addDocumentFailedRule(DocumentAnalysisRule.documentFailedRuleFrom(DocumentRule.R_TAX_ALL_LEAF));
            }

        } catch (Exception e) {
            log.warn("Exception during the income tax rules validation", e);
            report.setAnalysisStatus(DocumentAnalysisStatus.UNDEFINED);
        }
        return report;
    }
}