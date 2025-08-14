package fr.dossierfacile.process.file.service.documentrules;

import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.entity.ocr.ScholarshipFile;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.enums.ParsedFileClassification;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;

public class ScholarshipRulesValidationServiceTest {

    private final ScholarshipRulesValidationService service = new ScholarshipRulesValidationService();

    private File scholarshipFile(String firstName, String lastName, int startYear, int endYear, int annualAmount) {
        ScholarshipFile sf = ScholarshipFile.builder()
                .firstName(firstName)
                .lastName(lastName)
                .startYear(startYear)
                .endYear(endYear)
                .annualAmount(annualAmount)
                .build();
        ParsedFileAnalysis pfa = ParsedFileAnalysis.builder()
                .classification(ParsedFileClassification.SCHOLARSHIP)
                .parsedFile(sf)
                .build();
        return File.builder().parsedFileAnalysis(pfa).build();
    }

    private File nonScholarshipFile() {
        ParsedFileAnalysis pfa = ParsedFileAnalysis.builder()
                .classification(ParsedFileClassification.PAYSLIP)
                .parsedFile(null)
                .build();
        return File.builder().parsedFileAnalysis(pfa).build();
    }

    private Tenant tenant(String firstName, String lastName) {
        ApartmentSharing sharing = ApartmentSharing.builder().applicationType(ApplicationType.ALONE).build();
        Tenant t = Tenant.builder().firstName(firstName).lastName(lastName).apartmentSharing(sharing).build();
        sharing.setTenants(List.of(t));
        return t;
    }

    private Document baseDoc(List<File> files) {
        return Document.builder()
                .documentCategory(DocumentCategory.FINANCIAL)
                .documentSubCategory(DocumentSubCategory.SCHOLARSHIP)
                .files(files)
                .build();
    }

    private DocumentAnalysisReport emptyReport(Document d) {
        return DocumentAnalysisReport.builder()
                .document(d)
                .failedRules(new LinkedList<>())
                .passedRules(new LinkedList<>())
                .inconclusiveRules(new LinkedList<>())
                .build();
    }

    @Test
    void shouldBeApplied_true() {
        int y = LocalDate.now().getYear();
        Document doc = baseDoc(List.of(scholarshipFile("JEAN", "DUPONT", y-1, y, 1200)));
        doc.setTenant(tenant("JEAN", "DUPONT"));
        Assertions.assertThat(service.shouldBeApplied(doc)).isTrue();
    }

    @Test
    void shouldBeApplied_false_no_scholarship_file() {
        int y = LocalDate.now().getYear();
        Document doc = baseDoc(List.of(nonScholarshipFile()));
        doc.setTenant(tenant("JEAN", "DUPONT"));
        Assertions.assertThat(service.shouldBeApplied(doc)).isFalse();
    }

    @Test
    void process_all_rules_pass() {
        int y = LocalDate.now().getYear();
        Document doc = baseDoc(List.of(scholarshipFile("JEAN", "DUPONT", y-1, y+1, 10000)));
        doc.setMonthlySum(1000); // annual /12 = 1000
        doc.setTenant(tenant("JEAN", "DUPONT"));
        var report = emptyReport(doc);
        service.process(doc, report);
        Assertions.assertThat(report.getPassedRules()).extracting(DocumentAnalysisRule::getRule)
                .containsExactly(
                        DocumentRule.R_SCHOLARSHIP_PARSED,
                        DocumentRule.R_SCHOLARSHIP_NAME,
                        DocumentRule.R_SCHOLARSHIP_EXPIRED,
                        DocumentRule.R_SCHOLARSHIP_AMOUNT
                );
        Assertions.assertThat(report.getFailedRules()).isEmpty();
        Assertions.assertThat(report.getInconclusiveRules()).isEmpty();
    }

    @Test
    void process_inconclusive_first_rule_blocks() {
        int y = LocalDate.now().getYear();
        Document doc = baseDoc(List.of(nonScholarshipFile())); // no scholarship classification
        doc.setTenant(tenant("JEAN", "DUPONT"));
        doc.setMonthlySum(1000);
        var report = emptyReport(doc);
        service.process(doc, report);
        Assertions.assertThat(report.getInconclusiveRules()).extracting(DocumentAnalysisRule::getRule)
                .containsExactly(DocumentRule.R_SCHOLARSHIP_PARSED);
        Assertions.assertThat(report.getPassedRules()).isEmpty();
        Assertions.assertThat(report.getFailedRules()).isEmpty();
    }

    @Test
    void process_name_fail_year_and_amount_pass() {
        int y = LocalDate.now().getYear();
        Document doc = baseDoc(List.of(scholarshipFile("JEAN", "DURAND", y-1, y+1, 6000)));
        doc.setTenant(tenant("JEAN", "DUPONT")); // last name mismatch
        doc.setMonthlySum(600); // 6000 /10 = 600
        var report = emptyReport(doc);
        service.process(doc, report);
        Assertions.assertThat(report.getPassedRules()).extracting(DocumentAnalysisRule::getRule)
                .containsExactly(
                        DocumentRule.R_SCHOLARSHIP_PARSED,
                        DocumentRule.R_SCHOLARSHIP_EXPIRED,
                        DocumentRule.R_SCHOLARSHIP_AMOUNT
                );
        Assertions.assertThat(report.getFailedRules()).extracting(DocumentAnalysisRule::getRule)
                .containsExactly(DocumentRule.R_SCHOLARSHIP_NAME);
        Assertions.assertThat(report.getInconclusiveRules()).isEmpty();
    }

    @Test
    void process_year_fail_amount_pass() {
        int y = LocalDate.now().getYear();
        // endYear strictly less than current year => always fail year validity
        Document doc = baseDoc(List.of(scholarshipFile("JEAN", "DUPONT", y-3, y-1, 10000)));
        doc.setTenant(tenant("JEAN", "DUPONT"));
        doc.setMonthlySum(1000);
        var report = emptyReport(doc);
        service.process(doc, report);
        Assertions.assertThat(report.getPassedRules()).extracting(DocumentAnalysisRule::getRule)
                .containsExactly(
                        DocumentRule.R_SCHOLARSHIP_PARSED,
                        DocumentRule.R_SCHOLARSHIP_NAME,
                        DocumentRule.R_SCHOLARSHIP_AMOUNT
                );
        Assertions.assertThat(report.getFailedRules()).extracting(DocumentAnalysisRule::getRule)
                .containsExactly(DocumentRule.R_SCHOLARSHIP_EXPIRED);
    }

    @Test
    void process_amount_fail() {
        int y = LocalDate.now().getYear();
        // annual 12000 => avg 1000; monthlySum 1015 diff=15>10
        Document doc = baseDoc(List.of(scholarshipFile("JEAN", "DUPONT", y-1, y+1, 12000)));
        doc.setTenant(tenant("JEAN", "DUPONT"));
        doc.setMonthlySum(1015);
        var report = emptyReport(doc);
        service.process(doc, report);
        Assertions.assertThat(report.getPassedRules()).extracting(DocumentAnalysisRule::getRule)
                .containsExactly(
                        DocumentRule.R_SCHOLARSHIP_PARSED,
                        DocumentRule.R_SCHOLARSHIP_NAME,
                        DocumentRule.R_SCHOLARSHIP_EXPIRED
                );
        Assertions.assertThat(report.getFailedRules()).extracting(DocumentAnalysisRule::getRule)
                .containsExactly(DocumentRule.R_SCHOLARSHIP_AMOUNT);
    }
}

