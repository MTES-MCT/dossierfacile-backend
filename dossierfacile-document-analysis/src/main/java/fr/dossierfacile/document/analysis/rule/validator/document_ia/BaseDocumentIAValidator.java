package fr.dossierfacile.document.analysis.rule.validator.document_ia;


import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentIAFileAnalysis;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.enums.DocumentIAFileAnalysisStatus;
import fr.dossierfacile.document.analysis.rule.validator.AbstractDocumentRuleValidator;
import fr.dossierfacile.document.analysis.rule.validator.french_identity_card.document_ia_model.DocumentIdentity;

import java.util.List;
import java.util.Objects;

public abstract class BaseDocumentIAValidator extends AbstractDocumentRuleValidator {

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
            return new DocumentIdentity(List.of(document.getGuarantor().getFirstName()), document.getGuarantor().getLastName());
        }

        if (document.getTenant() != null) {
            return new DocumentIdentity(List.of(document.getTenant().getFirstName()), document.getTenant().getLastName(), document.getTenant().getPreferredName());
        }
        return null;
    }

}
