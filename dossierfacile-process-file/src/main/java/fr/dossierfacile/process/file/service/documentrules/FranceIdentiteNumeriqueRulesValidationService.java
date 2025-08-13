package fr.dossierfacile.process.file.service.documentrules;

import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.enums.ParsedFileAnalysisStatus;
import fr.dossierfacile.common.enums.ParsedFileClassification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

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
                    report.addDocumentFailedRule(DocumentAnalysisRule.documentFailedRuleFrom(DocumentRule.R_FRANCE_IDENTITE_STATUS));

                } else {
                    report.addDocumentPassedRule(DocumentAnalysisRule.documentPassedRuleFrom(DocumentRule.R_FRANCE_IDENTITE_STATUS));
                }

                // Fake rule
                if (parsedDocument != null && parsedDocument.getStatus() != null && parsedDocument.getStatus().equals("VALID")) {
                    report.addDocumentPassedRule(DocumentAnalysisRule.documentPassedRuleFrom(DocumentRule.R_FRANCE_IDENTITE_STATUS));
                } else {
                    report.addDocumentFailedRule(DocumentAnalysisRule.documentFailedRuleFrom(DocumentRule.R_FRANCE_IDENTITE_STATUS));
                }

                // TODO : check that France Identité verifies that names on pdf matches qrcode
                Person documentOwner = Optional.ofNullable((Person) document.getTenant()).orElseGet(document::getGuarantor);
                String firstName = documentOwner.getFirstName();

                if (checkNamesRule(parsedDocument, document)) {
                    report.addDocumentPassedRule(DocumentAnalysisRule.documentPassedRuleFrom(DocumentRule.R_FRANCE_IDENTITE_NAMES));
                } else {
                    log.info("Le nom/prenom ne correpond pas à l'utilisateur tenantId:{} firstname: {}", document.getTenant().getId(), firstName);
                    report.addDocumentFailedRule(DocumentAnalysisRule.documentFailedRuleFrom(DocumentRule.R_FRANCE_IDENTITE_NAMES));
                }
            }
        }
        return report;
    }

    private boolean checkNamesRule(@Nullable FranceIdentiteApiResult parsedDocument, Document document) {
        if (parsedDocument == null || parsedDocument.getAttributes() == null) {
            return false;
        }
        Person documentOwner = Optional.ofNullable((Person) document.getTenant()).orElseGet(document::getGuarantor);
        String firstName = documentOwner.getFirstName();
        String lastName = documentOwner.getLastName();
        return normalizeName(parsedDocument.getAttributes().getGivenName()).contains(normalizeName(firstName))
                && normalizeName(parsedDocument.getAttributes().getFamilyName()).contains(normalizeName(lastName));
    }
}