package fr.dossierfacile.common.domain.service;

import fr.dossierfacile.common.domain.model.document.Document;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentStatus;
import fr.dossierfacile.common.infrastructure.entity.DocumentEntity;
import fr.dossierfacile.common.infrastructure.repository.JpaDocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CheckDocumentForReprocessingDomainServiceTest {

    private CheckDocumentForReprocessingDomainService service;

    @Mock
    private JpaDocumentRepository jpaDocumentRepository;

    @Mock
    private MessagePublisher messagePublisher;

    @BeforeEach
    void setUp() {
        service = new CheckDocumentForReprocessingDomainService(jpaDocumentRepository, messagePublisher);
    }

    @Test
    void should_check_and_reset_documents_for_tenant() {
        Document document = new Document(DocumentEntity.builder().tenantId(1L).build());
        Document docToReset = spy(new Document(DocumentEntity.builder()
                .id(2L)
                .tenantId(1L)
                .noDocument(true)
                .documentCategory(DocumentCategory.PROFESSIONAL)
                .documentStatus(DocumentStatus.VALIDATED)
                .build()));

        when(jpaDocumentRepository.getDocumentsByTenantId(1L)).thenReturn(List.of(docToReset));

        service.checkDocumentsForReprocessing(document);

        verify(jpaDocumentRepository).getDocumentsByTenantId(1L);
        verify(docToReset).resetValidateOrInProgressDocumentAfterFileDeleted();
        verify(messagePublisher).sendDocumentForPdfGeneration(2L);
        verify(jpaDocumentRepository).save(docToReset);
    }

    @Test
    void should_check_and_reset_documents_for_guarantor() {
        Document document = new Document(DocumentEntity.builder().guarantorId(2L).build());
        Document docToReset = spy(new Document(DocumentEntity.builder()
                .id(3L)
                .guarantorId(2L)
                .noDocument(false)
                .documentCategory(DocumentCategory.FINANCIAL)
                .documentStatus(DocumentStatus.VALIDATED)
                .build()));

        when(jpaDocumentRepository.getDocumentsByGuarantorId(2L)).thenReturn(List.of(docToReset));

        service.checkDocumentsForReprocessing(document);

        verify(jpaDocumentRepository).getDocumentsByGuarantorId(2L);
        verify(docToReset).resetValidateOrInProgressDocumentAfterFileDeleted();
        verifyNoInteractions(messagePublisher);
        verify(jpaDocumentRepository).save(docToReset);
    }
}
