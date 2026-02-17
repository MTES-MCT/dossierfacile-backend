package fr.dossierfacile.document.analysis.rule.validator.document_ia;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.DocumentIAFileAnalysis;
import fr.dossierfacile.common.enums.DocumentIAFileAnalysisStatus;
import fr.dossierfacile.document.analysis.rule.validator.RuleValidatorOutput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HasBeenDocumentIAAnalysedBITest {

    private final HasBeenDocumentIAAnalysedBI validator = new HasBeenDocumentIAAnalysedBI();

    @Test
    void should_be_valid_when_no_files() {
        Document document = mock(Document.class);
        when(document.getFiles()).thenReturn(Collections.emptyList());

        RuleValidatorOutput output = validator.validate(document);
        assertTrue(output.isValid());
        assertEquals(RuleValidatorOutput.RuleLevel.PASSED, output.ruleLevel());
    }

    @Test
    void should_be_valid_when_files_have_no_analysis() {
        Document document = mock(Document.class);
        File file = mock(File.class);
        when(file.getDocumentIAFileAnalysis()).thenReturn(null);

        when(document.getFiles()).thenReturn(Collections.singletonList(file));

        RuleValidatorOutput output = validator.validate(document);
        assertTrue(output.isValid());
        assertEquals(RuleValidatorOutput.RuleLevel.PASSED, output.ruleLevel());
    }

    @Test
    void should_be_valid_when_all_analyses_are_success() {
        Document document = mock(Document.class);
        File file1 = mock(File.class);
        DocumentIAFileAnalysis analysis1 = mock(DocumentIAFileAnalysis.class);
        when(analysis1.getAnalysisStatus()).thenReturn(DocumentIAFileAnalysisStatus.SUCCESS);
        when(file1.getDocumentIAFileAnalysis()).thenReturn(analysis1);

        File file2 = mock(File.class);
        DocumentIAFileAnalysis analysis2 = mock(DocumentIAFileAnalysis.class);
        when(analysis2.getAnalysisStatus()).thenReturn(DocumentIAFileAnalysisStatus.SUCCESS);
        when(file2.getDocumentIAFileAnalysis()).thenReturn(analysis2);

        when(document.getFiles()).thenReturn(Arrays.asList(file1, file2));

        RuleValidatorOutput output = validator.validate(document);
        assertTrue(output.isValid());
        assertEquals(RuleValidatorOutput.RuleLevel.PASSED, output.ruleLevel());
    }

    @ParameterizedTest
    @EnumSource(value = DocumentIAFileAnalysisStatus.class, names = {"SUCCESS"}, mode = EnumSource.Mode.EXCLUDE)
    void should_be_invalid_when_one_analysis_is_not_success(DocumentIAFileAnalysisStatus status) {
        Document document = mock(Document.class);
        File file1 = mock(File.class);
        DocumentIAFileAnalysis analysis1 = mock(DocumentIAFileAnalysis.class);
        when(analysis1.getAnalysisStatus()).thenReturn(DocumentIAFileAnalysisStatus.SUCCESS);
        when(file1.getDocumentIAFileAnalysis()).thenReturn(analysis1);

        File file2 = mock(File.class);
        DocumentIAFileAnalysis analysis2 = mock(DocumentIAFileAnalysis.class);
        when(analysis2.getAnalysisStatus()).thenReturn(status);
        when(file2.getDocumentIAFileAnalysis()).thenReturn(analysis2);

        when(document.getFiles()).thenReturn(Arrays.asList(file1, file2));

        RuleValidatorOutput output = validator.validate(document);
        assertFalse(output.isValid());
        assertEquals(RuleValidatorOutput.RuleLevel.INCONCLUSIVE, output.ruleLevel());
    }

    @Test
    void should_be_invalid_when_all_analyses_are_failed() {
        Document document = mock(Document.class);
        File file1 = mock(File.class);
        DocumentIAFileAnalysis analysis1 = mock(DocumentIAFileAnalysis.class);
        when(analysis1.getAnalysisStatus()).thenReturn(DocumentIAFileAnalysisStatus.FAILED);
        when(file1.getDocumentIAFileAnalysis()).thenReturn(analysis1);

        File file2 = mock(File.class);
        DocumentIAFileAnalysis analysis2 = mock(DocumentIAFileAnalysis.class);
        when(analysis2.getAnalysisStatus()).thenReturn(DocumentIAFileAnalysisStatus.FAILED);
        when(file2.getDocumentIAFileAnalysis()).thenReturn(analysis2);

        when(document.getFiles()).thenReturn(Arrays.asList(file1, file2));

        RuleValidatorOutput output = validator.validate(document);
        assertFalse(output.isValid());
        assertEquals(RuleValidatorOutput.RuleLevel.INCONCLUSIVE, output.ruleLevel());
    }
}

