package fr.dossierfacile.document.analysis.service;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentAnalysisReport;
import fr.dossierfacile.common.entity.DocumentAnalysisRule;
import fr.dossierfacile.common.entity.DocumentAnalysisStatus;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.repository.DocumentAnalysisReportRepository;
import fr.dossierfacile.common.repository.DocumentCommonRepository;
import fr.dossierfacile.document.analysis.rule.AbstractRulesValidationService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentAnalysisServiceImplTest {

    @Mock
    private Map<DocumentSubCategory, AbstractRulesValidationService> mapOfValidators;
    @Mock
    private DocumentCommonRepository documentRepository;
    @Mock
    private DocumentAnalysisReportRepository documentAnalysisReportRepository;

    @InjectMocks
    private DocumentAnalysisServiceImpl documentAnalysisService;

    @Test
    void should_do_nothing_if_no_validator() {
        Document document = Document.builder()
                .documentSubCategory(DocumentSubCategory.MY_NAME)
                .build();
        when(mapOfValidators.get(DocumentSubCategory.MY_NAME)).thenReturn(null);

        documentAnalysisService.analyseDocument(document);

        verify(documentAnalysisReportRepository, never()).save(any());
        verify(documentRepository, never()).save(any());
    }

    @Test
    void should_analyse_document_status_denied() {
        Document document = Document.builder()
                .documentSubCategory(DocumentSubCategory.MY_NAME)
                .build();
        AbstractRulesValidationService validator = mock(AbstractRulesValidationService.class);

        when(mapOfValidators.get(DocumentSubCategory.MY_NAME)).thenReturn(validator);
        when(validator.process(eq(document), any(DocumentAnalysisReport.class)))
                .thenAnswer(invocation -> {
                    DocumentAnalysisReport report = invocation.getArgument(1);
                    report.setFailedRules(List.of(DocumentAnalysisRule.builder().rule(DocumentRule.R_TAX_PARSE).build()));
                    return report;
                });

        documentAnalysisService.analyseDocument(document);

        ArgumentCaptor<DocumentAnalysisReport> reportCaptor = ArgumentCaptor.forClass(DocumentAnalysisReport.class);
        verify(documentAnalysisReportRepository).save(reportCaptor.capture());
        DocumentAnalysisReport report = reportCaptor.getValue();

        Assertions.assertEquals(DocumentAnalysisStatus.DENIED, report.getAnalysisStatus());
        verify(documentRepository).save(document);
    }

    @Test
    void should_analyse_document_status_undefined_when_inconclusive() {
        Document document = Document.builder()
                .documentSubCategory(DocumentSubCategory.MY_NAME)
                .build();
        AbstractRulesValidationService validator = mock(AbstractRulesValidationService.class);

        when(mapOfValidators.get(DocumentSubCategory.MY_NAME)).thenReturn(validator);
        when(validator.process(eq(document), any(DocumentAnalysisReport.class)))
                .thenAnswer(invocation -> {
                    DocumentAnalysisReport report = invocation.getArgument(1);
                    report.setInconclusiveRules(List.of(DocumentAnalysisRule.builder().rule(DocumentRule.R_TAX_PARSE).build()));
                    return report;
                });

        documentAnalysisService.analyseDocument(document);

        ArgumentCaptor<DocumentAnalysisReport> reportCaptor = ArgumentCaptor.forClass(DocumentAnalysisReport.class);
        verify(documentAnalysisReportRepository).save(reportCaptor.capture());
        DocumentAnalysisReport report = reportCaptor.getValue();

        Assertions.assertEquals(DocumentAnalysisStatus.UNDEFINED, report.getAnalysisStatus());
        verify(documentRepository).save(document);
    }

    @Test
    void should_analyse_document_status_checked() {
        Document document = Document.builder()
                .documentSubCategory(DocumentSubCategory.MY_NAME)
                .build();
        AbstractRulesValidationService validator = mock(AbstractRulesValidationService.class);

        when(mapOfValidators.get(DocumentSubCategory.MY_NAME)).thenReturn(validator);
        when(validator.process(eq(document), any(DocumentAnalysisReport.class)))
                .thenAnswer(invocation -> {
                    DocumentAnalysisReport report = invocation.getArgument(1);
                    report.setPassedRules(List.of(DocumentAnalysisRule.builder().rule(DocumentRule.R_TAX_PARSE).build()));
                    return report;
                });

        documentAnalysisService.analyseDocument(document);

        ArgumentCaptor<DocumentAnalysisReport> reportCaptor = ArgumentCaptor.forClass(DocumentAnalysisReport.class);
        verify(documentAnalysisReportRepository).save(reportCaptor.capture());
        DocumentAnalysisReport report = reportCaptor.getValue();

        Assertions.assertEquals(DocumentAnalysisStatus.CHECKED, report.getAnalysisStatus());
        verify(documentRepository).save(document);
    }

    @Test
    void should_analyse_document_status_undefined_when_empty() {
        Document document = Document.builder()
                .documentSubCategory(DocumentSubCategory.MY_NAME)
                .build();
        AbstractRulesValidationService validator = mock(AbstractRulesValidationService.class);

        when(mapOfValidators.get(DocumentSubCategory.MY_NAME)).thenReturn(validator);
        when(validator.process(eq(document), any(DocumentAnalysisReport.class)))
                .thenAnswer(invocation -> {
                    DocumentAnalysisReport report = invocation.getArgument(1);
                    report.setFailedRules(Collections.emptyList());
                    report.setPassedRules(Collections.emptyList());
                    report.setInconclusiveRules(Collections.emptyList());
                    return report;
                });

        documentAnalysisService.analyseDocument(document);

        ArgumentCaptor<DocumentAnalysisReport> reportCaptor = ArgumentCaptor.forClass(DocumentAnalysisReport.class);
        verify(documentAnalysisReportRepository).save(reportCaptor.capture());
        DocumentAnalysisReport report = reportCaptor.getValue();

        Assertions.assertEquals(DocumentAnalysisStatus.UNDEFINED, report.getAnalysisStatus());
        verify(documentRepository).save(document);
    }

    @Test
    void should_reset_previous_report() {
        DocumentAnalysisReport existingReport = DocumentAnalysisReport.builder()
                .failedRules(List.of(DocumentAnalysisRule.builder().rule(DocumentRule.R_TAX_PARSE).build()))
                .analysisStatus(DocumentAnalysisStatus.DENIED)
                .build();
        Document document = Document.builder()
                .documentSubCategory(DocumentSubCategory.MY_NAME)
                .documentAnalysisReport(existingReport)
                .build();
        AbstractRulesValidationService validator = mock(AbstractRulesValidationService.class);

        when(mapOfValidators.get(DocumentSubCategory.MY_NAME)).thenReturn(validator);
        when(validator.process(eq(document), any(DocumentAnalysisReport.class)))
                .thenAnswer(invocation -> invocation.getArgument(1));

        documentAnalysisService.analyseDocument(document);

        ArgumentCaptor<DocumentAnalysisReport> reportCaptor = ArgumentCaptor.forClass(DocumentAnalysisReport.class);
        verify(documentAnalysisReportRepository).save(reportCaptor.capture());
        DocumentAnalysisReport report = reportCaptor.getValue();

        Assertions.assertEquals(existingReport, report);
        Assertions.assertTrue(report.getFailedRules().isEmpty());
        Assertions.assertEquals(DocumentAnalysisStatus.UNDEFINED, report.getAnalysisStatus());
    }
}
