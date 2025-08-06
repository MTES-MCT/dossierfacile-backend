package fr.dossierfacile.process.file.service.documentrules;

import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.entity.ocr.GuaranteeProviderFile;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.ParsedStatus;
import fr.dossierfacile.process.file.util.PersonNameComparator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static fr.dossierfacile.common.enums.DocumentSubCategory.OTHER_GUARANTEE;
import static fr.dossierfacile.common.enums.DocumentSubCategory.VISALE;

@Service
@RequiredArgsConstructor
@Slf4j
public class GuaranteeProviderRulesValidationService implements RulesValidationService {
    @Override
    public boolean shouldBeApplied(Document document) {
        return document.getDocumentCategory() == DocumentCategory.GUARANTEE_PROVIDER_CERTIFICATE
                && List.of(OTHER_GUARANTEE, VISALE).contains(document.getDocumentSubCategory())
                && !CollectionUtils.isEmpty(document.getFiles())
                && document.getFiles().stream().anyMatch((f) -> f.getParsedFileAnalysis() != null
                && f.getParsedFileAnalysis().getParsedFile() != null);
    }


    private boolean checkNamesRule(Document document, GuaranteeProviderFile parsedFile) {
        Tenant tenant = document.getGuarantor().getTenant();
        return parsedFile.getNames().stream().anyMatch(
                (fullname) -> PersonNameComparator.bearlyEqualsTo(fullname.firstName(), tenant.getFirstName())
                        && (PersonNameComparator.bearlyEqualsTo(fullname.lastName(), tenant.getLastName())
                        || PersonNameComparator.bearlyEqualsTo(fullname.lastName(), tenant.getPreferredName())));
    }

    private boolean checkValidityRule(GuaranteeProviderFile parsedFile) {
        LocalDate validityDate = LocalDate.parse(parsedFile.getValidityDate(), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        return validityDate.isAfter(LocalDate.now());
    }

    @Override
    public DocumentAnalysisReport process(Document document, DocumentAnalysisReport report) {

        try {
            if (CollectionUtils.isEmpty(document.getFiles())
                    || document.getFiles().get(0).getParsedFileAnalysis() == null
                    || document.getFiles().get(0).getParsedFileAnalysis().getParsedFile() == null) {
                report.setAnalysisStatus(DocumentAnalysisStatus.UNDEFINED);
                return report;
            }

            GuaranteeProviderFile parsedFile = (GuaranteeProviderFile) document.getFiles().get(0).getParsedFileAnalysis().getParsedFile();
            if (parsedFile.getStatus() == ParsedStatus.INCOMPLETE) {
                log.error("Document was not correctly parsed :" + document.getGuarantor().getTenant().getId());
                report.setAnalysisStatus(DocumentAnalysisStatus.UNDEFINED);
            } else if (!checkNamesRule(document, parsedFile)) {
                log.error("Document names mismatches :" + document.getGuarantor().getTenant().getId());
                report.addDocumentBrokenRule(DocumentBrokenRule.of(DocumentRule.R_GUARANTEE_NAMES));
                report.setAnalysisStatus(DocumentAnalysisStatus.DENIED);
            } else if (!checkValidityRule(parsedFile)) {
                log.error("Document is expired :" + document.getGuarantor().getTenant().getId());
                report.addDocumentBrokenRule(DocumentBrokenRule.of(DocumentRule.R_GUARANTEE_EXPIRED));
                report.setAnalysisStatus(DocumentAnalysisStatus.DENIED);
            } else {
                report.setAnalysisStatus(DocumentAnalysisStatus.CHECKED);
            }

        } catch (Exception e) {
            log.error("Error during the rules validation execution pocess", e);
            report.setAnalysisStatus(DocumentAnalysisStatus.UNDEFINED);
        }
        return report;
    }

}