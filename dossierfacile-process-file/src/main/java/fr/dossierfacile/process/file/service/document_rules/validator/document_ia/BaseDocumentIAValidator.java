package fr.dossierfacile.process.file.service.document_rules.validator.document_ia;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentIAFileAnalysis;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.enums.DocumentIAFileAnalysisStatus;
import fr.dossierfacile.process.file.service.document_rules.validator.AbstractDocumentRuleValidator;

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
