package fr.dossierfacile.process.file.service.documentrules;

import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.entity.ocr.ScholarshipFile;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.enums.ParsedFileClassification;
import fr.dossierfacile.common.exceptions.NotFoundException;
import fr.dossierfacile.process.file.util.PersonNameComparator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScholarshipRulesValidationService implements RulesValidationService {

    @Override
    public boolean shouldBeApplied(Document document) {
        return document.getDocumentCategory() == DocumentCategory.FINANCIAL
                && document.getDocumentSubCategory() == DocumentSubCategory.SCHOLARSHIP
                && !CollectionUtils.isEmpty(document.getFiles())
                && document.getFiles().stream().anyMatch((f) -> f.getParsedFileAnalysis() != null
                && f.getParsedFileAnalysis().getParsedFile() != null
                && f.getParsedFileAnalysis().getParsedFile().getClassification() == ParsedFileClassification.SCHOLARSHIP);
    }

    protected boolean checkNamesRule(ScholarshipFile scholarshipFile, Document document) {
        Person documentOwner = Optional.ofNullable((Person) document.getTenant()).orElseGet(() -> document.getGuarantor());

        return PersonNameComparator.bearlyEqualsTo(scholarshipFile.getFirstName(), documentOwner.getFirstName())
                && (PersonNameComparator.bearlyEqualsTo(scholarshipFile.getLastName(), documentOwner.getLastName())
                || PersonNameComparator.bearlyEqualsTo(scholarshipFile.getLastName(), documentOwner.getPreferredName()));
    }

    private boolean checkYearValidityRule(ScholarshipFile scholarshipFile, Document document) {
        LocalDate now = LocalDate.now();
        if (now.isBefore(LocalDate.of(now.getYear(), 9, 15))) {
            return scholarshipFile.getEndYear() >= now.getYear();
        }
        return scholarshipFile.getEndYear() > now.getYear();
    }

    private boolean checkAmountValidityRule(ScholarshipFile scholarshipFile, Document document) {
        double monthlyAverage = ((double) scholarshipFile.getAnnualAmount()) / 10.0;
        // Check amount difference
        double diffAmount = Math.abs(monthlyAverage - document.getMonthlySum());
        return (diffAmount <= 10);
    }

    @Override
    public DocumentAnalysisReport process(Document document, DocumentAnalysisReport report) {

        try {
            ScholarshipFile scholarshipFile = document.getFiles().stream()
                    .filter(file -> ParsedFileClassification.SCHOLARSHIP == file.getParsedFileAnalysis().getClassification())
                    .map(file -> (ScholarshipFile) file.getParsedFileAnalysis().getParsedFile())
                    .findFirst().orElseThrow(NotFoundException::new);

            if (!checkNamesRule(scholarshipFile, document)) {
                log.error("Document names mismatches :" + document.getId());
                report.addDocumentBrokenRule(DocumentBrokenRule.of(DocumentRule.R_SCHOLARSHIP_NAME));
                report.setAnalysisStatus(DocumentAnalysisStatus.DENIED);
            } else if (!checkYearValidityRule(scholarshipFile, document)) {
                log.error("Document is expired :" + document.getId());
                report.addDocumentBrokenRule(DocumentBrokenRule.of(DocumentRule.R_SCHOLARSHIP_EXPIRED));
                report.setAnalysisStatus(DocumentAnalysisStatus.DENIED);
            } else if (!checkAmountValidityRule(scholarshipFile, document)) {
                log.error("Amount specified on document mismatch :" + document.getId());
                report.addDocumentBrokenRule(DocumentBrokenRule.of(DocumentRule.R_SCHOLARSHIP_AMOUNT));
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