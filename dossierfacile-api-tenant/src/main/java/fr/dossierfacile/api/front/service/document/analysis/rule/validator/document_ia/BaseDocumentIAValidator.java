package fr.dossierfacile.api.front.service.document.analysis.rule.validator.document_ia;

import fr.dossierfacile.api.front.service.document.analysis.rule.validator.AbstractDocumentRuleValidator;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentIAFileAnalysis;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.enums.DocumentIAFileAnalysisStatus;

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

}
