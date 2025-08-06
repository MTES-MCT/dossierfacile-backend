package fr.dossierfacile.process.file.service.documentrules;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentAnalysisReport;
import fr.dossierfacile.common.entity.DocumentAnalysisStatus;
import fr.dossierfacile.common.entity.DocumentBrokenRule;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.FranceIdentiteApiResult;
import fr.dossierfacile.common.entity.ParsedFileAnalysis;
import fr.dossierfacile.common.entity.Person;
import fr.dossierfacile.common.enums.ParsedFileAnalysisStatus;
import fr.dossierfacile.common.enums.ParsedFileClassification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static fr.dossierfacile.common.enums.DocumentSubCategory.FRANCE_IDENTITE;
import static fr.dossierfacile.process.file.util.NameUtil.normalizeName;

@Service
@RequiredArgsConstructor
@Slf4j
public class FranceIdentiteNumeriqueRulesValidationService implements RulesValidationService {

    @Override
    public boolean shouldBeApplied(Document document) {
        return document.getDocumentSubCategory() == FRANCE_IDENTITE;
    }

    @Override
    public DocumentAnalysisReport process(Document document, DocumentAnalysisReport report) {
        boolean parsingOk = false;
        for (File dfFile : document.getFiles()) {
            ParsedFileAnalysis analysis = dfFile.getParsedFileAnalysis();
            if (analysis == null || analysis.getAnalysisStatus() == ParsedFileAnalysisStatus.FAILED) {
                continue;
            }
            if (analysis.getClassification() == ParsedFileClassification.FRANCE_IDENTITE) {
                FranceIdentiteApiResult parsedDocument = (FranceIdentiteApiResult) analysis.getParsedFile();

                // Parse Rule
                if (parsedDocument == null
                        || "ERROR_FILE".equals(parsedDocument.getStatus())
                        || parsedDocument.getStatus() == null
                        || parsedDocument.getAttributes().getFamilyName() == null
                        || parsedDocument.getAttributes().getGivenName() == null
                        || parsedDocument.getAttributes().getValidityDate() == null) {
                    report.addDocumentBrokenRule(DocumentBrokenRule.of(DocumentRule.R_FRANCE_IDENTITE_STATUS));
                    continue;
                }

                // Fake Rule
                if (!("VALID".equals(parsedDocument.getStatus()))) {
                    report.addDocumentBrokenRule(DocumentBrokenRule.of(DocumentRule.R_FRANCE_IDENTITE_STATUS));
                    continue;
                }

                // TODO : check that France Identité verifies that names on pdf matches qrcode
                Person documentOwner = Optional.ofNullable((Person) document.getTenant()).orElseGet(document::getGuarantor);
                String firstName = documentOwner.getFirstName();
                String lastName = documentOwner.getLastName();
                if (!(normalizeName(parsedDocument.getAttributes().getGivenName()).contains(normalizeName(firstName))
                        && (normalizeName(parsedDocument.getAttributes().getFamilyName()).contains(normalizeName(lastName)))
                )) {
                    log.error("Le nom/prenom ne correpond pas à l'utilisateur tenantId:" + document.getTenant().getId() + " firstname: " + firstName);
                    report.addDocumentBrokenRule(DocumentBrokenRule.of(DocumentRule.R_FRANCE_IDENTITE_NAMES));
                }
                parsingOk = true;
            }
        }
        var brokenRules = report.getBrokenRules();
        if (brokenRules != null && brokenRules.isEmpty() && parsingOk ) {
            report.setAnalysisStatus(DocumentAnalysisStatus.CHECKED);
        } else if (brokenRules != null && brokenRules.stream().anyMatch(r -> r.getRule().getLevel() == DocumentRule.Level.CRITICAL)) {
            report.setAnalysisStatus(DocumentAnalysisStatus.DENIED);
        } else {
            report.setAnalysisStatus(DocumentAnalysisStatus.UNDEFINED);
        }
        return report;
    }
}