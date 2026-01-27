package fr.dossierfacile.document.analysis.rule.validator.document_ia;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.enums.DocumentIAFileAnalysisStatus;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

@Slf4j
public class HasBeenDocumentIAAnalysedBI extends BaseDocumentIAValidator {

    @Override
    protected boolean isBlocking() {
        return true;
    }

    @Override
    protected boolean isInconclusive() {
        return true;
    }

    @Override
    protected DocumentRule getRule() {
        return DocumentRule.R_DOCUMENT_IA_ANALYSED;
    }

    @Override
    protected boolean isValid(Document document) {
        return document.getFiles().stream()
                .map(File::getDocumentIAFileAnalysis)
                .filter(Objects::nonNull)
                .noneMatch(it -> it.getAnalysisStatus() != DocumentIAFileAnalysisStatus.SUCCESS);
    }
}