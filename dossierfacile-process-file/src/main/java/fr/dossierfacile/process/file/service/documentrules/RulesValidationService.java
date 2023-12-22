package fr.dossierfacile.process.file.service.documentrules;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentAnalysisReport;

public interface RulesValidationService {
    boolean shouldBeApplied(Document document);
    DocumentAnalysisReport process(Document document, DocumentAnalysisReport report);
}