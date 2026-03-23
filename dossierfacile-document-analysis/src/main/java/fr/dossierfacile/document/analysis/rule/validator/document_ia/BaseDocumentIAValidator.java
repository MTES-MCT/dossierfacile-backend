package fr.dossierfacile.document.analysis.rule.validator.document_ia;


import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentIAFileAnalysis;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.enums.DocumentIAFileAnalysisStatus;
import fr.dossierfacile.document.analysis.rule.validator.AbstractDocumentRuleValidator;
import fr.dossierfacile.document.analysis.rule.validator.french_identity_card.document_ia_model.DocumentIdentity;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public abstract class BaseDocumentIAValidator extends AbstractDocumentRuleValidator {

    // On utilise ce Pattern au lieu de " " pour gérer les éspaces insécables et les multi espaces
    public static final Pattern TOKEN_SEPARATOR = Pattern.compile("[\\s\\u00A0]+");

    protected List<DocumentIAFileAnalysis> getSuccessfulDocumentIAAnalyses(Document document) {
        return document.getFiles().stream()
                .map(File::getDocumentIAFileAnalysis)
                .filter(Objects::nonNull)
                .filter(it -> it.getAnalysisStatus() == DocumentIAFileAnalysisStatus.SUCCESS)
                .toList();
    }

    protected boolean hasAnyNonSuccessfulDocumentIAAnalyses(Document document) {
        return !document.getFiles().stream()
                .map(File::getDocumentIAFileAnalysis)
                .filter(Objects::nonNull)
                .allMatch(it -> it.getAnalysisStatus() == DocumentIAFileAnalysisStatus.SUCCESS);
    }

    protected DocumentIdentity getNamesFromDocument(Document document) {
        if (document.getGuarantor() != null) {
            return new DocumentIdentity(Arrays.stream(TOKEN_SEPARATOR.split(document.getGuarantor().getFirstName())).toList(), document.getGuarantor().getLastName());
        }

        if (document.getTenant() != null) {
            return new DocumentIdentity(Arrays.stream(TOKEN_SEPARATOR.split(document.getTenant().getFirstName())).toList(), document.getTenant().getLastName(), document.getTenant().getPreferredName());
        }
        return null;
    }

}
