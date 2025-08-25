package fr.dossierfacile.process.file.service.document_rules;

import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.entity.ocr.GuaranteeProviderFile;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.enums.ParsedFileClassification;
import fr.dossierfacile.common.enums.ParsedStatus;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;

class GuaranteeProviderRulesValidationServiceTest {

    private final GuaranteeProviderRulesValidationService service = new GuaranteeProviderRulesValidationService();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private File gpFile(ParsedStatus status, String validityDate, GuaranteeProviderFile.FullName... names) {
        GuaranteeProviderFile gpf = GuaranteeProviderFile.builder()
                .status(status)
                .validityDate(validityDate)
                .names(List.of(names))
                .build();
        ParsedFileAnalysis pfa = ParsedFileAnalysis.builder()
                .classification(ParsedFileClassification.GUARANTEE_PROVIDER)
                .parsedFile(gpf)
                .build();
        return File.builder().parsedFileAnalysis(pfa).build();
    }

    private File noParsedFile() {
        return File.builder().build();
    }

    private File nullParsedInner() {
        ParsedFileAnalysis pfa = ParsedFileAnalysis.builder()
                .classification(ParsedFileClassification.GUARANTEE_PROVIDER)
                .parsedFile(null)
                .build();
        return File.builder().parsedFileAnalysis(pfa).build();
    }

    private Guarantor guarantorWithTenant(String first, String last) {
        Tenant t = Tenant.builder().firstName(first).lastName(last).build();
        return Guarantor.builder().tenant(t).build();
    }

    private Document baseDoc(List<File> files) {
        return Document.builder()
                .documentCategory(DocumentCategory.GUARANTEE_PROVIDER_CERTIFICATE)
                .documentSubCategory(DocumentSubCategory.OTHER_GUARANTEE)
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
        Document doc = baseDoc(List.of(gpFile(ParsedStatus.COMPLETE, LocalDate.now().plusDays(5).format(FMT), new GuaranteeProviderFile.FullName("JEAN", "DUPONT"))));
        doc.setGuarantor(guarantorWithTenant("JEAN", "DUPONT"));
        Assertions.assertThat(service.shouldBeApplied(doc)).isTrue();
    }

    @Test
    void shouldBeApplied_false_wrong_category() {
        Document doc = Document.builder()
                .documentCategory(DocumentCategory.FINANCIAL)
                .documentSubCategory(DocumentSubCategory.OTHER_GUARANTEE)
                .files(List.of(gpFile(ParsedStatus.COMPLETE, LocalDate.now().plusDays(5).format(FMT), new GuaranteeProviderFile.FullName("A", "B"))))
                .build();
        doc.setGuarantor(guarantorWithTenant("JEAN", "DUPONT"));
        Assertions.assertThat(service.shouldBeApplied(doc)).isFalse();
    }

    @Test
    void shouldBeApplied_false_no_parsed_content() {
        Document doc = baseDoc(List.of(noParsedFile()));
        doc.setGuarantor(guarantorWithTenant("JEAN", "DUPONT"));
        Assertions.assertThat(service.shouldBeApplied(doc)).isFalse();
    }

    @Test
    void process_all_rules_pass_when_everything_valid() {
        Document doc = baseDoc(List.of(gpFile(ParsedStatus.COMPLETE,
                LocalDate.now().plusDays(10).format(FMT),
                new GuaranteeProviderFile.FullName("JEAN", "DUPONT"))));
        doc.setGuarantor(guarantorWithTenant("JEAN", "DUPONT"));
        var report = emptyReport(doc);
        service.process(doc, report);
        Assertions.assertThat(report.getPassedRules()).extracting(DocumentAnalysisRule::getRule)
                .containsExactly(
                        DocumentRule.R_GUARANTEE_PARSING,
                        DocumentRule.R_GUARANTEE_NAMES,
                        DocumentRule.R_GUARANTEE_EXPIRED
                );
        Assertions.assertThat(report.getFailedRules()).isEmpty();
        Assertions.assertThat(report.getInconclusiveRules()).isEmpty();
    }

    @Test
    void process_stops_after_first_rule_inconclusive_when_no_parsed() {
        Document doc = baseDoc(List.of(noParsedFile()));
        doc.setGuarantor(guarantorWithTenant("JEAN", "DUPONT"));
        var report = emptyReport(doc);
        service.process(doc, report);
        Assertions.assertThat(report.getInconclusiveRules()).extracting(DocumentAnalysisRule::getRule)
                .containsExactly(DocumentRule.R_GUARANTEE_PARSING);
        Assertions.assertThat(report.getPassedRules()).isEmpty();
        Assertions.assertThat(report.getFailedRules()).isEmpty();
    }

    @Test
    void process_name_rule_fails_then_year_rule_still_evaluated() {
        Document doc = baseDoc(List.of(gpFile(ParsedStatus.COMPLETE,
                LocalDate.now().plusDays(5).format(FMT),
                new GuaranteeProviderFile.FullName("PAUL", "MARTIN")))); // ne correspond pas au tenant
        doc.setGuarantor(guarantorWithTenant("JEAN", "DUPONT"));
        var report = emptyReport(doc);
        service.process(doc, report);
        Assertions.assertThat(report.getPassedRules()).extracting(DocumentAnalysisRule::getRule)
                .containsExactly(DocumentRule.R_GUARANTEE_PARSING, DocumentRule.R_GUARANTEE_EXPIRED);
        Assertions.assertThat(report.getFailedRules()).extracting(DocumentAnalysisRule::getRule)
                .containsExactly(DocumentRule.R_GUARANTEE_NAMES); // YEAR doit passer car date future
        Assertions.assertThat(report.getInconclusiveRules()).isEmpty();
    }

    @Test
    void process_year_rule_fails_with_past_date() {
        Document doc = baseDoc(List.of(gpFile(ParsedStatus.COMPLETE,
                LocalDate.now().minusDays(1).format(FMT),
                new GuaranteeProviderFile.FullName("JEAN", "DUPONT"))));
        doc.setGuarantor(guarantorWithTenant("JEAN", "DUPONT"));
        var report = emptyReport(doc);
        service.process(doc, report);
        Assertions.assertThat(report.getPassedRules()).extracting(DocumentAnalysisRule::getRule)
                .containsExactly(
                        DocumentRule.R_GUARANTEE_PARSING,
                        DocumentRule.R_GUARANTEE_NAMES
                );
        Assertions.assertThat(report.getFailedRules()).extracting(DocumentAnalysisRule::getRule)
                .containsExactly(DocumentRule.R_GUARANTEE_EXPIRED);
        Assertions.assertThat(report.getInconclusiveRules()).isEmpty();
    }

    @Test
    void process_inconclusive_when_parsed_analysis_null() {
        Document doc = baseDoc(List.of(nullParsedInner()));
        doc.setGuarantor(guarantorWithTenant("JEAN", "DUPONT"));
        var report = emptyReport(doc);
        service.process(doc, report);
        Assertions.assertThat(report.getInconclusiveRules()).extracting(DocumentAnalysisRule::getRule)
                .containsExactly(DocumentRule.R_GUARANTEE_PARSING);
    }
}
