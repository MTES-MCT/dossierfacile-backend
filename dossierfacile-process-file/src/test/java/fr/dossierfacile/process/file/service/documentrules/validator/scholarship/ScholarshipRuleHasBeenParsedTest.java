package fr.dossierfacile.process.file.service.documentrules.validator.scholarship;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.ParsedFileAnalysis;
import fr.dossierfacile.common.entity.ocr.ScholarshipFile;
import fr.dossierfacile.common.entity.ocr.PayslipFile;
import fr.dossierfacile.common.enums.ParsedFileAnalysisStatus;
import fr.dossierfacile.common.enums.ParsedFileClassification;
import fr.dossierfacile.process.file.service.documentrules.validator.RuleValidatorOutput;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Year;
import java.time.YearMonth;
import java.util.List;

public class ScholarshipRuleHasBeenParsedTest {

    private File scholarshipFile() {
        ScholarshipFile sf = ScholarshipFile.builder()
                .classification(ParsedFileClassification.SCHOLARSHIP)
                .firstName("JOHN")
                .lastName("DOE")
                .annualAmount(1000)
                .startYear(Year.now().minusYears(1).getValue())
                .endYear(Year.now().getValue())
                .build();
        ParsedFileAnalysis analysis = ParsedFileAnalysis.builder()
                .analysisStatus(ParsedFileAnalysisStatus.COMPLETED)
                .classification(ParsedFileClassification.SCHOLARSHIP)
                .parsedFile(sf)
                .build();
        return File.builder().parsedFileAnalysis(analysis).build();
    }

    private File nonScholarshipFile() {
        PayslipFile payslip = PayslipFile.builder()
                .classification(ParsedFileClassification.PAYSLIP)
                .fullname("DOE JOHN")
                .month(YearMonth.now().minusMonths(1))
                .netTaxableIncome(500.0)
                .cumulativeNetTaxableIncome(500.0)
                .build();
        ParsedFileAnalysis analysis = ParsedFileAnalysis.builder()
                .analysisStatus(ParsedFileAnalysisStatus.COMPLETED)
                .classification(ParsedFileClassification.PAYSLIP)
                .parsedFile(payslip)
                .build();
        return File.builder().parsedFileAnalysis(analysis).build();
    }

    private File fileWithoutAnalysis() {
        return File.builder().build();
    }

    private RuleValidatorOutput validate(Document d) {
        return new ScholarshipRuleHasBeenParsed().validate(d);
    }

    @Test
    void passes_when_at_least_one_scholarship_file_present() {
        Document doc = Document.builder()
                .files(List.of(nonScholarshipFile(), scholarshipFile()))
                .build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isTrue();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
        Assertions.assertThat(out.rule().getRule()).isEqualTo(DocumentRule.R_SCHOLARSHIP_PARSED);
    }

    @Test
    void inconclusive_when_no_scholarship_classification_found() {
        Document doc = Document.builder()
                .files(List.of(nonScholarshipFile(), nonScholarshipFile()))
                .build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isFalse();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
        Assertions.assertThat(out.rule().getRule()).isEqualTo(DocumentRule.R_SCHOLARSHIP_PARSED);
    }

    @Test
    void inconclusive_when_no_parsed_analysis() {
        Document doc = Document.builder()
                .files(List.of(fileWithoutAnalysis()))
                .build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isFalse();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
    }
}

