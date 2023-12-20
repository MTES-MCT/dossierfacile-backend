package fr.dossierfacile.process.file.service.documentrules;

import fr.dossierfacile.common.entity.BarCodeDocumentType;
import fr.dossierfacile.common.entity.BarCodeFileAnalysis;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentAnalysisReport;
import fr.dossierfacile.common.entity.DocumentAnalysisStatus;
import fr.dossierfacile.common.entity.DocumentBrokenRule;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.ParsedFileAnalysis;
import fr.dossierfacile.common.entity.Person;
import fr.dossierfacile.common.entity.ocr.TaxIncomeLeaf;
import fr.dossierfacile.common.entity.ocr.TaxIncomeMainFile;
import fr.dossierfacile.common.enums.ParsedFileAnalysisStatus;
import fr.dossierfacile.common.enums.ParsedFileClassification;
import fr.dossierfacile.process.file.barcode.twoddoc.parsing.TwoDDocDataType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class IncomeTaxRulesValidationService implements RulesValidationService {

    private TaxIncomeMainFile fromQR(BarCodeFileAnalysis barCodeFileAnalysis) {
        Map<String, String> dataWithLabel = (Map<String, String>) barCodeFileAnalysis.getVerifiedData();
        return TaxIncomeMainFile.builder()
                .declarant1NumFiscal(dataWithLabel.get(TwoDDocDataType.ID_47.getLabel()))
                .declarant1Nom(dataWithLabel.get(TwoDDocDataType.ID_46.getLabel()))
                .declarant2NumFiscal(dataWithLabel.get(TwoDDocDataType.ID_49.getLabel()))
                .declarant2Nom(dataWithLabel.get(TwoDDocDataType.ID_48.getLabel()))
                .anneeDesRevenus(dataWithLabel.get(TwoDDocDataType.ID_45.getLabel()))
                .nombreDeParts(dataWithLabel.get(TwoDDocDataType.ID_43.getLabel()))
                .dateDeMiseEnRecouvrement(dataWithLabel.get(TwoDDocDataType.ID_4A.getLabel()))
                .revenuFiscalDeReference(dataWithLabel.get(TwoDDocDataType.ID_41.getLabel()))
                .numeroFiscalDeclarant1(dataWithLabel.get(TwoDDocDataType.ID_47.getLabel()))
                .numeroFiscalDeclarant2(dataWithLabel.get(TwoDDocDataType.ID_49.getLabel()))
                .referenceAvis(dataWithLabel.get(TwoDDocDataType.ID_44.getLabel())).build();
    }

    private static String normalizeName(String name) {
        if (name == null)
            return null;
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD);
        return normalized.replace('-', ' ')
                .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "").toUpperCase().trim();
    }

    @Override
    @Transactional
    public DocumentAnalysisReport process(Document document) {
        DocumentAnalysisReport report = DocumentAnalysisReport.builder().document(document).build();
        List<DocumentBrokenRule> brokenRules = Optional.ofNullable(report.getBrokenRules())
                .orElseGet(() -> {
                    report.setBrokenRules(new LinkedList<>());
                    return report.getBrokenRules();
                });

        // Parse Rule
        for (File dfFile : document.getFiles()) {
            ParsedFileAnalysis analysis = dfFile.getParsedFileAnalysis();
            if (analysis == null || analysis.getAnalysisStatus() == ParsedFileAnalysisStatus.FAILED) {
                break;
            }
            if (analysis.getClassification() == ParsedFileClassification.TAX_INCOME) {
                TaxIncomeMainFile parsedDocument = (TaxIncomeMainFile) analysis.getParsedFile();

                if (parsedDocument == null
                        || parsedDocument.getAnneeDesRevenus() == null
                        || parsedDocument.getDeclarant1Nom() == null
                        || parsedDocument.getRevenuFiscalDeReference() == null) {
                    brokenRules.add(DocumentBrokenRule.builder()
                            .rule(DocumentRule.R_TAX_PARSE)
                            .message(DocumentRule.R_TAX_PARSE.getDefaultMessage())
                            .build());
                }
            }
        }

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
                        && (!qrDocument.getAnneeDesRevenus().equalsIgnoreCase(parsedDocument.getAnneeDesRevenus())
                        || !qrDocument.getDeclarant1Nom().equalsIgnoreCase(parsedDocument.getDeclarant1Nom())
                        || !qrDocument.getRevenuFiscalDeReference().equalsIgnoreCase(parsedDocument.getRevenuFiscalDeReference().replaceAll("\\s", ""))
                )) {
                    log.error("Le 2DDoc code ne correspond pas au contenu du document tenantId:" + document.getTenant().getId());
                    brokenRules.add(DocumentBrokenRule.builder()
                            .rule(DocumentRule.R_TAX_FAKE)
                            .message(DocumentRule.R_TAX_FAKE.getDefaultMessage())
                            .build());
                }
            }
        }

        // Provide N-1 tax income / not more N-3
        List<Integer> providedYears = new ArrayList<>(2);
        for (File dfFile : document.getFiles()) {
            if (dfFile.getFileAnalysis() != null) {
                TaxIncomeMainFile qrDocument = fromQR(dfFile.getFileAnalysis());
                if (qrDocument.getAnneeDesRevenus() != null)
                    providedYears.add(Integer.parseInt(qrDocument.getAnneeDesRevenus()));
            }
        }
        Integer maxYear = providedYears.stream().max(Integer::compare).orElse(null);
        // try to check some rules
        LocalDate now = LocalDate.now();
        if (now.isBefore(LocalDate.of(now.getYear(), 9, 15))) {
            if (maxYear != (now.getYear() - 1) && maxYear != (now.getYear() - 2)) {
                log.error("Tax income is deprecated - tenantId:" + document.getTenant().getId());
                brokenRules.add(DocumentBrokenRule.builder()
                        .rule(DocumentRule.R_TAX_N1)
                        .message("L'avis d'impots sur les revenus " + maxYear
                                + ", vous devez fournir un avis d'impots sur les revenus " + (now.getYear() - 1)
                                + " ou " + (now.getYear() - 2))
                        .build());
            }
        } else if (maxYear != (now.getYear() - 1)) {
            log.error("Tax income is deprecated - tenantId:" + document.getTenant().getId());
            brokenRules.add(DocumentBrokenRule.builder()
                    .rule(DocumentRule.R_TAX_N1)
                    .message("L'avis d'impots sur les revenus " + maxYear
                            + " n'est pas accepté, vous devez fournir un avis d'impots sur les revenus " + (now.getYear() - 1))
                    .build());
        }

        // N-3 check
        Integer minYear = providedYears.stream().min(Integer::compare).orElse(null);
        int authorisedYear = now.minusMonths(9).minusDays(15).getYear();
        if (minYear < (authorisedYear - 2)) {
            log.error("Tax income is deprecated - tenantId:" + document.getTenant().getId());
            brokenRules.add(DocumentBrokenRule.builder()
                    .rule(DocumentRule.R_TAX_N3)
                    .message(DocumentRule.R_TAX_N3.getDefaultMessage())
                    .build());
        }

        // Firstname LastName
        Person documentOwner = Optional.ofNullable((Person) document.getTenant()).orElseGet(() -> document.getGuarantor());
        String firstName = documentOwner.getFirstName();
        String lastName = documentOwner.getLastName();
        for (File dfFile : document.getFiles()) {
            BarCodeFileAnalysis analysis = dfFile.getFileAnalysis();
            if (analysis.getDocumentType() == BarCodeDocumentType.TAX_ASSESSMENT) {
                TaxIncomeMainFile qrDocument = fromQR(dfFile.getFileAnalysis());

                if (!(normalizeName(qrDocument.getDeclarant1Nom()).contains(normalizeName(firstName))
                        && normalizeName(qrDocument.getDeclarant1Nom()).contains(normalizeName(lastName))
                        || (qrDocument.getDeclarant2Nom() != null &&
                        normalizeName(qrDocument.getDeclarant2Nom()).contains(normalizeName(firstName))
                        && normalizeName(qrDocument.getDeclarant2Nom()).contains(normalizeName(lastName))
                ))) {
                    log.error("Le nom/prenom ne correpond pas à l'uilitsation tenantId:" + document.getTenant().getId() + " firstname: " + firstName);
                    brokenRules.add(DocumentBrokenRule.builder()
                            .rule(DocumentRule.R_TAX_NAMES)
                            .message(DocumentRule.R_TAX_NAMES.getDefaultMessage())
                            .build());
                }
            }
        }

        // missing pages - currently only check when page are linked in mainFile
        // TODO later - rebuild the entire document from external taxincome leaf
        for (File dfFile : document.getFiles()) {
            ParsedFileAnalysis analysis = dfFile.getParsedFileAnalysis();
            if (analysis == null || analysis.getAnalysisStatus() == ParsedFileAnalysisStatus.FAILED) {
                break;
            }
            if (analysis.getClassification() == ParsedFileClassification.TAX_INCOME) {
                TaxIncomeMainFile parsedDocument = (TaxIncomeMainFile) analysis.getParsedFile();
                if (CollectionUtils.isEmpty(parsedDocument.getTaxIncomeLeaves())) {
                    log.error("Income tax has not incometaxleaf:" + document.getTenant().getId());
                    brokenRules.add(DocumentBrokenRule.builder()
                            .rule(DocumentRule.R_TAX_LEAF)
                            .message(DocumentRule.R_TAX_LEAF.getDefaultMessage())
                            .build());
                    break;
                }
                TaxIncomeLeaf leaf = parsedDocument.getTaxIncomeLeaves().get(0);
                if (leaf.getPageCount() != null && leaf.getPageCount() != parsedDocument.getTaxIncomeLeaves().size()) {
                    log.error("Income tax has not ALL incometaxleaf:" + document.getTenant().getId());
                    brokenRules.add(DocumentBrokenRule.builder()
                            .rule(DocumentRule.R_TAX_ALL_LEAF)
                            .message(DocumentRule.R_TAX_ALL_LEAF.getDefaultMessage())
                            .build());
                }

            }
        }

        if (brokenRules.isEmpty()) {
            report.setAnalysisStatus(DocumentAnalysisStatus.CHECKED);
        } else if (brokenRules.stream().anyMatch(r -> r.getRule().getLevel() == DocumentRule.Level.CRITICAL)) {
            report.setAnalysisStatus(DocumentAnalysisStatus.DENIED);
        } else {
            report.setAnalysisStatus(DocumentAnalysisStatus.UNDEFINED);
        }

        return report;
    }
}