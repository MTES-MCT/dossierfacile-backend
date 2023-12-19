package fr.dossierfacile.process.file.service.documentrules;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentAnalysisReport;
import fr.dossierfacile.common.entity.DocumentAnalysisStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class GuaranteeProviderRulesValidationService implements RulesValidationService {
    @Override
    public DocumentAnalysisReport process(Document document) {
        log.debug("TODO");
        // TODO Currently there is not implemented rules
        return DocumentAnalysisReport.builder().analysisStatus(DocumentAnalysisStatus.UNDEFINED).document(document).build();
    }
}