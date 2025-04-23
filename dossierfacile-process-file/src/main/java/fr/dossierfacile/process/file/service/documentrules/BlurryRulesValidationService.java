package fr.dossierfacile.process.file.service.documentrules;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentAnalysisReport;
import fr.dossierfacile.common.entity.DocumentAnalysisStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class BlurryRulesValidationService implements RulesValidationService {

    @Override
    public boolean shouldBeApplied(Document document) {
        return !CollectionUtils.isEmpty(document.getFiles());
    }

    @Override
    public DocumentAnalysisReport process(Document document, DocumentAnalysisReport report) {
        document.getFiles().forEach(file -> {

        });

//                report.getBrokenRules().add(DocumentBrokenRule.builder()
//                        .rule(DocumentRule.R_SCHOLARSHIP_NAME)
//                        .message(DocumentRule.R_SCHOLARSHIP_NAME.getDefaultMessage())
//                        .build());
        report.setAnalysisStatus(DocumentAnalysisStatus.DENIED);
        return report;
    }

}