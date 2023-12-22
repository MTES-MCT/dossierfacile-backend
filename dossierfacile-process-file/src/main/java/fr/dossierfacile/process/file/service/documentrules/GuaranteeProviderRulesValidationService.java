package fr.dossierfacile.process.file.service.documentrules;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentAnalysisReport;
import fr.dossierfacile.common.entity.DocumentAnalysisStatus;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class GuaranteeProviderRulesValidationService implements RulesValidationService {
    @Override
    public boolean shouldBeApplied(Document document){
        return document.getDocumentCategory() == DocumentCategory.IDENTIFICATION
                && document.getDocumentSubCategory() == DocumentSubCategory.CERTIFICATE_VISA;
    }
    @Override
    public DocumentAnalysisReport process(Document document, DocumentAnalysisReport report) {
        log.debug("TODO");
        // TODO Currently there is not implemented rules
        return DocumentAnalysisReport.builder().analysisStatus(DocumentAnalysisStatus.UNDEFINED).document(document).build();
    }
}