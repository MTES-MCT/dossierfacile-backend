package fr.dossierfacile.document.analysis.listener;

import fr.dossierfacile.common.domain.event.DocumentModifiedEvent;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.repository.DocumentCommonRepository;
import fr.dossierfacile.document.analysis.service.DocumentIAService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentModifiedListenerTest {

    private DocumentModifiedListener listener;

    @Mock
    private DocumentCommonRepository documentRepository;

    @Mock
    private DocumentIAService documentIAService;

    @BeforeEach
    void setUp() {
        listener = new DocumentModifiedListener(documentRepository, documentIAService);
    }

    @Test
    void should_trigger_ia_analysis_when_document_exists() {
        Document document = Document.builder().id(123L).build();
        when(documentRepository.findById(123L)).thenReturn(Optional.of(document));

        DocumentModifiedEvent event = new DocumentModifiedEvent(123L);
        listener.onDocumentModified(event);

        verify(documentIAService).analyseDocument(document);
    }

    @Test
    void should_not_trigger_ia_analysis_when_document_does_not_exist() {
        when(documentRepository.findById(123L)).thenReturn(Optional.empty());

        DocumentModifiedEvent event = new DocumentModifiedEvent(123L);
        listener.onDocumentModified(event);

        verifyNoInteractions(documentIAService);
    }
}
